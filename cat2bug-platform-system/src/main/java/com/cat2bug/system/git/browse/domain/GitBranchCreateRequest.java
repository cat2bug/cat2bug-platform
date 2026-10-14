package com.cat2bug.system.git.browse.domain;

/**
 * 创建分支请求。
 */
public class GitBranchCreateRequest
{
    private String name;

    private String fromRef;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFromRef()
    {
        return fromRef;
    }

    public void setFromRef(String fromRef)
    {
        this.fromRef = fromRef;
    }
}
