package com.cat2bug.quarkus.system.git.release;

import com.cat2bug.quarkus.common.config.Cat2BugConfig;

import java.io.File;

/**
 * 发行版附件在 {@code cat2bug.profile} 下的相对路径布局。
 *
 * <p>形态：{@code git/releases/{repositoryId}/{releaseId}/}（design.md D2）。</p>
 */
public final class GitReleaseStoragePaths
{
    private static final String SEGMENT = "git/releases";

    private GitReleaseStoragePaths()
    {
    }

    /**
     * 相对 profile 的目录（正斜杠，无末尾斜杠）。
     */
    public static String releaseAssetDir(Long repositoryId, Long releaseId)
    {
        return SEGMENT + "/" + repositoryId + "/" + releaseId;
    }

    /**
     * 相对 profile 的存储路径（写入 {@code sys_git_release_asset.storage_path}）。
     */
    public static String storagePath(Long repositoryId, Long releaseId, String storedFileName)
    {
        return releaseAssetDir(repositoryId, releaseId) + "/" + storedFileName;
    }

    /**
     * 磁盘绝对路径。
     */
    public static File absoluteFile(String storagePath)
    {
        String profile = Cat2BugConfig.getProfile();
        String normalized = storagePath.replace('/', File.separatorChar);
        return new File(profile, normalized);
    }

    /**
     * 上传基目录（绝对路径，含 release 目录）。
     */
    public static File releaseAssetAbsoluteDir(Long repositoryId, Long releaseId)
    {
        return absoluteFile(releaseAssetDir(repositoryId, releaseId));
    }
}
