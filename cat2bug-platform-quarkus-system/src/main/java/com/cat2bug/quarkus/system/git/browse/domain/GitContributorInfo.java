package com.cat2bug.quarkus.system.git.browse.domain;

/**
 * 仓库贡献者（按 Git 提交作者聚合），对应 OpenSpec {@code git-repo-contributors}。
 */
public class GitContributorInfo
{
    private String displayName;

    private String email;

    private int commitCount;

    /** 匹配到的平台用户 ID，未匹配时为 {@code null} */
    private Long userId;

    /** 匹配到的平台头像 URL，未匹配时为 {@code null} */
    private String avatar;

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public int getCommitCount()
    {
        return commitCount;
    }

    public void setCommitCount(int commitCount)
    {
        this.commitCount = commitCount;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }
}
