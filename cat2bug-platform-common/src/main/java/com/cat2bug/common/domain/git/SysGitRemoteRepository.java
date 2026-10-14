package com.cat2bug.common.domain.git;

/**
 * 远端平台仓库摘要（列仓 API 响应元素；不持久化）。
 */
public class SysGitRemoteRepository
{
    private String name;

    /** 如 {@code group/repo} 或 {@code owner/repo} */
    private String fullPath;

    private String cloneUrl;

    private String defaultBranch;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFullPath()
    {
        return fullPath;
    }

    public void setFullPath(String fullPath)
    {
        this.fullPath = fullPath;
    }

    public String getCloneUrl()
    {
        return cloneUrl;
    }

    public void setCloneUrl(String cloneUrl)
    {
        this.cloneUrl = cloneUrl;
    }

    public String getDefaultBranch()
    {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch)
    {
        this.defaultBranch = defaultBranch;
    }
}
