package com.cat2bug.system.service.git;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * 基于 JGit 的 bare 仓库磁盘操作：init / clone / move / wipe。
 *
 * 约束（design.md D1 / tasks 2.2-2.4）：全程只用 {@code org.eclipse.jgit}，
 * 不通过 {@link ProcessBuilder} 拉起系统 {@code git} 进程。
 */
@Component
public class JGitBareRepositoryHelper
{
    /** 无用户名时 PAT 的默认用户名（GitLab oauth2 / 多数 HTTPS PAT 场景通用） */
    private static final String DEFAULT_CREDENTIAL_USERNAME = "oauth2";

    /**
     * 在指定磁盘路径初始化一个 bare 仓库。
     *
     * @param diskPath 目标磁盘绝对路径（父目录须已确保可写，见 {@link GitPathValidator#ensureParentWritable}）
     * @param defaultBranch 初始默认分支名（如 master）；为空则使用 JGit 默认值
     */
    public void initBareRepository(String diskPath, String defaultBranch)
    {
        File repoDir = new File(diskPath);
        try
        {
            InitCommand init = Git.init().setBare(true).setDirectory(repoDir);
            if (defaultBranch != null && !defaultBranch.isEmpty())
            {
                init.setInitialBranch(defaultBranch);
            }
            try (Git git = init.call())
            {
                // no-op：bare 仓创建即完成，首次 push 前无需额外提交
            }
        }
        catch (Exception e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.init-failed")).setDetailMessage(e.getMessage());
        }
    }

    /**
     * 从远端 URL bare clone 到本地磁盘路径（一次性导入；凭据仅本次请求作用域）。
     *
     * @param sourceUrl 远端 HTTPS/HTTP Git URL
     * @param diskPath 目标 bare 仓绝对路径
     * @param username 可选；为空且 token 非空时默认 {@code oauth2}
     * @param token 可选 PAT / 密码；为空则匿名 clone（公开仓）
     */
    public void cloneBareRepository(String sourceUrl, String diskPath, String username, String token)
    {
        File repoDir = new File(diskPath);
        try
        {
            CloneCommand clone = Git.cloneRepository()
                .setURI(sourceUrl)
                .setDirectory(repoDir)
                .setBare(true)
                .setCloneAllBranches(true);
            if (StringUtils.isNotEmpty(token))
            {
                String user = StringUtils.isNotEmpty(username) ? username : DEFAULT_CREDENTIAL_USERNAME;
                clone.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, token));
            }
            try (Git git = clone.call())
            {
                // bare clone 完成后与远端脱钩；不再保留凭据
            }
        }
        catch (TransportException e)
        {
            throw mapTransportException(e);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof TransportException)
            {
                throw mapTransportException((TransportException) cause);
            }
            if (isAuthFailureMessage(e.getMessage()) || (cause != null && isAuthFailureMessage(cause.getMessage())))
            {
                throw new ServiceException(MessageUtils.message("git.repo.import.auth-failed"))
                    .setDetailMessage(safeDetail(e));
            }
            throw new ServiceException(MessageUtils.message("git.repo.import.clone-failed"))
                .setDetailMessage(safeDetail(e));
        }
    }

    /**
     * 读取 bare 仓 HEAD 指向的本地分支名（无则返回 null）。
     */
    public String resolveDefaultBranch(String diskPath)
    {
        File repoDir = new File(diskPath);
        if (!repoDir.exists())
        {
            return null;
        }
        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(repoDir)
            .setBare()
            .build())
        {
            Ref head = repository.exactRef(Constants.HEAD);
            if (head != null && head.isSymbolic())
            {
                String target = head.getTarget().getName();
                if (target != null && target.startsWith(Constants.R_HEADS))
                {
                    return target.substring(Constants.R_HEADS.length());
                }
            }
            return repository.getBranch();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static ServiceException mapTransportException(TransportException e)
    {
        String message = e.getMessage();
        if (isAuthFailureMessage(message))
        {
            return new ServiceException(MessageUtils.message("git.repo.import.auth-failed"))
                .setDetailMessage(safeDetail(e));
        }
        return new ServiceException(MessageUtils.message("git.repo.import.network-failed"))
            .setDetailMessage(safeDetail(e));
    }

    private static boolean isAuthFailureMessage(String message)
    {
        if (message == null)
        {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("not authorized")
            || lower.contains("authentication is required")
            || lower.contains("auth fail")
            || lower.contains("401")
            || lower.contains("403")
            || lower.contains("invalid credentials")
            || lower.contains("access denied");
    }

    /** 详情消息不得包含凭据；仅保留异常类型与短消息。 */
    private static String safeDetail(Throwable e)
    {
        if (e == null)
        {
            return null;
        }
        String msg = e.getMessage();
        if (msg == null)
        {
            return e.getClass().getSimpleName();
        }
        // 防御：若远端错误串意外带回 query/token，截断并去敏感片段
        String sanitized = msg.replaceAll("(?i)(token|password|access_token|private-token)=[^\\s&]+", "$1=***");
        if (sanitized.length() > 200)
        {
            sanitized = sanitized.substring(0, 200);
        }
        return e.getClass().getSimpleName() + ": " + sanitized;
    }

    /**
     * 将 bare 仓库从旧磁盘路径迁移到新磁盘路径（slug 重命名 / 显式改路径场景）。
     *
     * 优先尝试原子 rename；跨文件系统等场景下回退为「递归复制 + 删除源目录」。
     */
    public void moveBareRepository(String oldDiskPath, String newDiskPath)
    {
        File oldDir = new File(oldDiskPath);
        File newDir = new File(newDiskPath);
        if (!oldDir.exists())
        {
            throw new ServiceException(MessageUtils.message("git.repo.move-source-missing"));
        }
        if (newDir.exists())
        {
            throw new ServiceException(MessageUtils.message("git.repo.move-target-exists"));
        }
        try
        {
            File newParent = newDir.getParentFile();
            if (newParent != null && !newParent.exists() && !newParent.mkdirs())
            {
                throw new IOException("mkdirs failed: " + newParent);
            }
            if (!oldDir.renameTo(newDir))
            {
                // 跨文件系统等 renameTo 失败场景，退化为拷贝后删除源目录
                FileUtils.copyDirectory(oldDir, newDir);
                FileUtils.deleteDirectory(oldDir);
            }
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.move-failed")).setDetailMessage(e.getMessage());
        }
    }

    /**
     * 从磁盘彻底删除 bare 仓库目录（DELETE {@code wipeDisk=true} 场景）。
     * 目录不存在视为成功（幂等）。
     */
    public void wipeBareRepository(String diskPath)
    {
        File repoDir = new File(diskPath);
        if (!repoDir.exists())
        {
            return;
        }
        try
        {
            FileUtils.deleteDirectory(repoDir);
        }
        catch (IOException e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.wipe-failed")).setDetailMessage(e.getMessage());
        }
    }

    /**
     * 判断磁盘路径下是否已经是一个合法的 bare 仓库（用于诊断/校验，不参与 CRUD 主流程）。
     */
    public boolean isValidBareRepository(String diskPath)
    {
        File repoDir = new File(diskPath);
        if (!repoDir.exists())
        {
            return false;
        }
        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(repoDir)
            .setBare()
            .build())
        {
            return repository.getObjectDatabase().exists();
        }
        catch (IOException e)
        {
            return false;
        }
    }
}
