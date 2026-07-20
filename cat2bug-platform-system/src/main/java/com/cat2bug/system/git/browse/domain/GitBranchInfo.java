package com.cat2bug.system.git.browse.domain;

/**
 * 分支信息（{@code Requirement: Browse repository contents via API} — List branches）
 */
public class GitBranchInfo
{
    private String name;

    private boolean isDefault;

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
}
