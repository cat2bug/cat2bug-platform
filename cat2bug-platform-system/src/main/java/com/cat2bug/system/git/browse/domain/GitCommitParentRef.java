package com.cat2bug.system.git.browse.domain;

/**
 * 提交详情中的父提交引用。
 */
public class GitCommitParentRef
{
    private String id;

    private String shortId;

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
}
