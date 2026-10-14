package com.cat2bug.system.git.browse.domain;

/**
 * 单次提交的文件与行级变更汇总。
 */
public class GitCommitChangeStats
{
    private int files;

    private int additions;

    private int deletions;

    public int getFiles()
    {
        return files;
    }

    public void setFiles(int files)
    {
        this.files = files;
    }

    public int getAdditions()
    {
        return additions;
    }

    public void setAdditions(int additions)
    {
        this.additions = additions;
    }

    public int getDeletions()
    {
        return deletions;
    }

    public void setDeletions(int deletions)
    {
        this.deletions = deletions;
    }
}
