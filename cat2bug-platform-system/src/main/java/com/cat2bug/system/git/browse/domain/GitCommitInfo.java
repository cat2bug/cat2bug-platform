package com.cat2bug.system.git.browse.domain;

import java.util.Date;

/**
 * 提交记录（{@code Requirement: Browse repository contents via API} — Commit log）
 */
public class GitCommitInfo
{
    private String id;

    private String shortId;

    private String message;

    private String authorName;

    private String authorEmail;

    private Date commitTime;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getShortId()
    {
        return shortId;
    }

    public void setShortId(String shortId)
    {
        this.shortId = shortId;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
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
}
