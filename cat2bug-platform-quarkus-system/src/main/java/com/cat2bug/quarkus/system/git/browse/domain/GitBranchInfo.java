package com.cat2bug.quarkus.system.git.browse.domain;

import java.util.Date;

/**
 * 分支信息（{@code Requirement: Browse repository contents via API} — List branches）
 */
public class GitBranchInfo
{
    private String name;

    private boolean isDefault;

    /** tip 提交的短 SHA */
    private String shortId;

    private String authorName;

    private String authorEmail;

    private Date commitTime;

    /** 相对默认分支独有的提交数 */
    private int ahead;

    /** 默认分支相对当前分支独有的提交数 */
    private int behind;

    public GitBranchInfo()
    {
    }

    public GitBranchInfo(String name, boolean isDefault)
    {
        this.name = name;
        this.isDefault = isDefault;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isDefault()
    {
        return isDefault;
    }

    public void setDefault(boolean isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getShortId()
    {
        return shortId;
    }

    public void setShortId(String shortId)
    {
        this.shortId = shortId;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getAuthorEmail()
    {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail)
    {
        this.authorEmail = authorEmail;
    }

    public Date getCommitTime()
    {
        return commitTime;
    }

    public void setCommitTime(Date commitTime)
    {
        this.commitTime = commitTime;
    }

    public int getAhead()
    {
        return ahead;
    }

    public void setAhead(int ahead)
    {
        this.ahead = ahead;
    }

    public int getBehind()
    {
        return behind;
    }

    public void setBehind(int behind)
    {
        this.behind = behind;
    }
}
