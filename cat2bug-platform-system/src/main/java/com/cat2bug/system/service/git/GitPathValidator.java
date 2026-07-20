package com.cat2bug.system.service.git;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Git 仓库磁盘路径与 slug 校验器。
 *
 * 职责（对应 design.md D6 / tasks 2.2）：
 * <ul>
 *     <li>校验 slug 格式，拼装默认磁盘路径 {@code {cat2bug.git.root}/{projectId}/{slug}.git}</li>
 *     <li>规范化 manage 用户提交的绝对路径，拒绝包含 {@code ..} 的路径穿越写法</li>
 *     <li>最小黑名单：拒绝根目录及若干系统关键目录，避免服务端进程被诱导写穿系统盘</li>
 *     <li>校验目标父目录可写（不存在则尝试创建）</li>
 * </ul>
 *
 * 不做符号链接穿透的深度检测（如 realpath 逃逸黑名单目录）；MVP 仅覆盖显而易见的误配置/恶意输入，
 * 生产部署仍应限制运行账号对系统目录的写权限作为纵深防御。
 */
@Component
public class GitPathValidator
{
    /** slug：字母/数字开头结尾，中间允许字母数字与 . _ -，长度 1~100 */
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-zA-Z0-9](?:[a-zA-Z0-9._-]{0,98}[a-zA-Z0-9])?$");

    /** 精确匹配即拒绝的目录（本身或其任意子路径均拒绝） */
    private static final Set<String> DENY_ROOTS = new HashSet<>(Arrays.asList(
        "/", "/etc", "/System", "/bin", "/sbin", "/usr", "/proc", "/dev", "/sys",
        "/boot", "/root", "/private", "/Library", "/Applications", "/var/db"
    ));

    /**
     * 校验并返回规范化后的 slug。
     */
    public String validateSlug(String slug)
    {
        if (StringUtils.isEmpty(slug))
        {
            throw new ServiceException(MessageUtils.message("git.repo.slug-required"));
        }
        String trimmed = slug.trim();
        if (!SLUG_PATTERN.matcher(trimmed).matches())
        {
            throw new ServiceException(MessageUtils.message("git.repo.slug-invalid"));
        }
        return trimmed;
    }

    /**
     * 默认磁盘路径：{@code {cat2bug.git.root}/{projectId}/{slug}.git}（绝对路径）。
     */
    public String buildDefaultDiskPath(Long projectId, String slug)
    {
        return Paths.get(Cat2BugConfig.getGitRootPath(), String.valueOf(projectId), slug + ".git")
            .toAbsolutePath()
            .normalize()
            .toString();
    }

    /**
     * 将库中存储的 {@code disk_path} 解析为绝对路径。
     * 历史数据可能存相对路径；相对路径按当前进程工作目录转绝对并 normalize。
     */
    public String resolveStoredDiskPath(String diskPath)
    {
        if (StringUtils.isEmpty(diskPath))
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-required"));
        }
        Path path = Paths.get(diskPath.trim());
        if (!path.isAbsolute())
        {
            path = path.toAbsolutePath();
        }
        return path.normalize().toString();
    }

    /**
     * 规范化并校验 manage 用户提交的绝对磁盘路径：
     * <ol>
     *     <li>非空、必须是绝对路径</li>
     *     <li>原始输入不允许包含 {@code ..} 路径段（无论是否绝对路径，一律拒绝，避免拼接穿越）</li>
     *     <li>normalize 之后不能落在黑名单目录（本身或子路径）</li>
     * </ol>
     *
     * @return 规范化后的绝对路径字符串
     */
    public String normalizeAndValidateAbsolutePath(String rawPath)
    {
        if (StringUtils.isEmpty(rawPath))
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-required"));
        }
        String trimmed = rawPath.trim();
        rejectTraversalSegments(trimmed);

        Path path;
        try
        {
            path = Paths.get(trimmed);
        }
        catch (Exception e)
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-invalid"));
        }
        if (!path.isAbsolute())
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-must-be-absolute"));
        }

        String normalized = path.normalize().toString();
        assertNotDenied(normalized);
        return normalized;
    }

    /**
     * 校验目标磁盘路径的父目录可写：不存在则尝试创建；创建失败或不可写均拒绝。
     */
    public void ensureParentWritable(String diskPath)
    {
        File repoDir = new File(diskPath);
        File parent = repoDir.getParentFile();
        if (parent == null)
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-invalid"));
        }
        if (!parent.exists() && !parent.mkdirs())
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-not-writable"));
        }
        if (!parent.canWrite())
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-not-writable"));
        }
        if (repoDir.exists() && !repoDir.canWrite())
        {
            throw new ServiceException(MessageUtils.message("git.repo.path-not-writable"));
        }
    }

    private void rejectTraversalSegments(String rawPath)
    {
        String normalizedSeparators = rawPath.replace('\\', '/');
        for (String segment : normalizedSeparators.split("/"))
        {
            if ("..".equals(segment))
            {
                throw new ServiceException(MessageUtils.message("git.repo.path-traversal-rejected"));
            }
        }
    }

    private void assertNotDenied(String normalizedPath)
    {
        for (String denied : DENY_ROOTS)
        {
            if (normalizedPath.equals(denied) || normalizedPath.startsWith(denied + File.separator))
            {
                throw new ServiceException(MessageUtils.message("git.repo.path-denied"));
            }
        }
    }
}
