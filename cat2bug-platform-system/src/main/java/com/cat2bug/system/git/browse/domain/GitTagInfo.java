package com.cat2bug.system.git.browse.domain;

import java.util.Date;

/**
 * Git 标签列表项（refs/tags/*，含可选发行版绑定）。
 */
public class GitTagInfo
{
    private String name;

    /** 是否为 annotated tag */
    private boolean annotated;

    /** annotated tag 消息；轻量 tag 为空串 */
    private String message;

    private String commitId;

    private String shortId;

    /** tip commit 短消息 */
    private String subject;

    private Date commitTime;

    private String authorName;

    private String authorEmail;

    /** 同名 {@code sys_git_release} 存在时填充 */
    private Long releaseId;

    public GitTagInfo()
    {
    }

    public GitTagInfo(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isAnnotated()
    {
        return annotated;
    }

    public void setAnnotated(boolean annotated)
    {
        this.annotated = annotated;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getCommitId()
    {
        return commitId;
    }

    public void setCommitId(String commitId)
    {
        this.commitId = commitId;
    }

    public String getShortId()
    {
        return shortId;
    }

    public void setShortId(String shortId)
    {
        this.shortId = shortId;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public Date getCommitTime()
    {
        return commitTime;
    }

    public void setCommitTime(Date commitTime)
    {
        this.commitTime = commitTime;
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

    public Long getReleaseId()
    {
        return releaseId;
    }

    public void setReleaseId(Long releaseId)
    {
        this.releaseId = releaseId;
    }
}
