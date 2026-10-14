package com.cat2bug.quarkus.system.git.browse.domain;

/**
 * 提交详情中的单文件变更（含可选 unified patch）。
 */
public class GitCommitFileChange
{
    private String path;

    /** RENAME / COPY 时的原路径 */
    private String oldPath;

    /** ADD / MODIFY / DELETE / RENAME / COPY */
    private String changeType;

    private int additions;

    private int deletions;

    private boolean binary;

    private boolean truncated;

    /** unified diff；binary 或 truncated 时为 null */
    private String patch;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getOldPath()
    {
        return oldPath;
    }

    public void setOldPath(String oldPath)
    {
        this.oldPath = oldPath;
    }

    public String getChangeType()
    {
        return changeType;
    }

    public void setChangeType(String changeType)
    {
        this.changeType = changeType;
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

    public boolean isBinary()
    {
        return binary;
    }

    public void setBinary(boolean binary)
    {
        this.binary = binary;
    }

    public boolean isTruncated()
    {
        return truncated;
    }

    public void setTruncated(boolean truncated)
    {
        this.truncated = truncated;
    }

    public String getPatch()
    {
        return patch;
    }

    public void setPatch(String patch)
    {
        this.patch = patch;
    }
}
