package com.cat2bug.system.git.browse.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 单条提交详情（全文消息 + 相对 first parent 的文件 diff）。
 */
public class GitCommitDetail
{
    private String id;

    private String shortId;

    private String fullMessage;

    private String authorName;

    private String authorEmail;

    private String committerName;

    private String committerEmail;

    private Date commitTime;

    private List<GitCommitParentRef> parents = new ArrayList<>();

    private GitCommitChangeStats stats;

    private List<GitCommitFileChange> files = new ArrayList<>();

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

    public String getFullMessage()
    {
        return fullMessage;
    }

    public void setFullMessage(String fullMessage)
    {
        this.fullMessage = fullMessage;
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

    public String getCommitterName()
    {
        return committerName;
    }

    public void setCommitterName(String committerName)
    {
        this.committerName = committerName;
    }

    public String getCommitterEmail()
    {
        return committerEmail;
    }

    public void setCommitterEmail(String committerEmail)
    {
        this.committerEmail = committerEmail;
    }

    public Date getCommitTime()
    {
        return commitTime;
    }

    public void setCommitTime(Date commitTime)
    {
        this.commitTime = commitTime;
    }

    public List<GitCommitParentRef> getParents()
    {
        return parents;
    }

    public void setParents(List<GitCommitParentRef> parents)
    {
        this.parents = parents;
    }

    public GitCommitChangeStats getStats()
    {
        return stats;
    }

    public void setStats(GitCommitChangeStats stats)
    {
        this.stats = stats;
    }

    public List<GitCommitFileChange> getFiles()
    {
        return files;
    }

    public void setFiles(List<GitCommitFileChange> files)
    {
        this.files = files;
    }
}
