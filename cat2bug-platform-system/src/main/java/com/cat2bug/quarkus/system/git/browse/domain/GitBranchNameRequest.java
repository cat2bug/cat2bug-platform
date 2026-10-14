package com.cat2bug.quarkus.system.git.browse.domain;

/**
 * 分支名称请求。
 */
public class GitBranchNameRequest
{
    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
