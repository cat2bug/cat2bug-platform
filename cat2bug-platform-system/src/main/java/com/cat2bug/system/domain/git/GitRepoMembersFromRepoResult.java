package com.cat2bug.system.domain.git;

/**
 * 从其他仓库批量导入成员结果。
 */
public class GitRepoMembersFromRepoResult
{
    private int added;

    private int skipped;

    public GitRepoMembersFromRepoResult()
    {
    }

    public GitRepoMembersFromRepoResult(int added, int skipped)
    {
        this.added = added;
        this.skipped = skipped;
    }

    public int getAdded()
    {
        return added;
    }

    public void setAdded(int added)
    {
        this.added = added;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public void setSkipped(int skipped)
    {
        this.skipped = skipped;
    }
}
