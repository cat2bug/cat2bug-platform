package com.cat2bug.system.git.browse.domain;

/**
 * 文件内容（{@code Requirement: Browse repository contents via API} — Read file content）
 *
 * <p>二进制文件不返回 {@link #content}，仅返回 {@link #binary}=true 与 {@link #size}，
 * 由前端展示"二进制文件不支持预览"（文档化的二进制处理响应，满足 spec 场景要求）。</p>
 */
public class GitBlobContent
{
    private String path;

    private boolean binary;

    /** 文本内容；binary=true 时为 null */
    private String content;

    private long size;

    /** 内容因过大被截断 */
    private boolean truncated;

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public boolean isBinary()
    {
        return binary;
    }

    public void setBinary(boolean binary)
    {
        this.binary = binary;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    public boolean isTruncated()
    {
        return truncated;
    }

    public void setTruncated(boolean truncated)
    {
        this.truncated = truncated;
    }
}
