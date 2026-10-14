package com.cat2bug.system.git.browse.domain;

import java.util.Date;

/**
 * 目录条目（{@code Requirement: Browse repository contents via API} — List directory）
 */
public class GitTreeEntryInfo
{
    /** tree=目录，blob=文件 */
    public static final String TYPE_TREE = "tree";
    public static final String TYPE_BLOB = "blob";

    private String name;

    private String path;

    private String type;

    private String objectId;

    /** blob 大小（字节），tree 为 null */
    private Long size;

    /** 该路径最近一次变更的提交短说明（展示用，可为空） */
    private String lastCommitMessage;

    /** 该路径最近一次变更的提交短 ID */
    private String lastCommitShortId;

    /** 该路径最近一次变更的作者 */
    private String lastCommitAuthor;

    /** 该路径最近一次变更时间 */
    private Date lastCommitTime;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getObjectId()
    {
        return objectId;
    }

    public void setObjectId(String objectId)
    {
        this.objectId = objectId;
    }

    public Long getSize()
    {
        return size;
    }

    public void setSize(Long size)
    {
        this.size = size;
    }

    public String getLastCommitMessage()
    {
        return lastCommitMessage;
    }

    public void setLastCommitMessage(String lastCommitMessage)
    {
        this.lastCommitMessage = lastCommitMessage;
    }

    public String getLastCommitShortId()
    {
        return lastCommitShortId;
    }

    public void setLastCommitShortId(String lastCommitShortId)
    {
        this.lastCommitShortId = lastCommitShortId;
    }

    public String getLastCommitAuthor()
    {
        return lastCommitAuthor;
    }

    public void setLastCommitAuthor(String lastCommitAuthor)
    {
        this.lastCommitAuthor = lastCommitAuthor;
    }

    public Date getLastCommitTime()
    {
        return lastCommitTime;
    }

    public void setLastCommitTime(Date lastCommitTime)
    {
        this.lastCommitTime = lastCommitTime;
    }
}
