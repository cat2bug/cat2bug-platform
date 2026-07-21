package com.cat2bug.system.service.git;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.MessageUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * 基于 JGit 的 bare 仓库磁盘操作：init / move / wipe。
 *
 * 约束（design.md D1 / tasks 2.2-2.4）：全程只用 {@code org.eclipse.jgit}，
 * 不通过 {@link ProcessBuilder} 拉起系统 {@code git} 进程。
 */
@Component
public class JGitBareRepositoryHelper
{
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
