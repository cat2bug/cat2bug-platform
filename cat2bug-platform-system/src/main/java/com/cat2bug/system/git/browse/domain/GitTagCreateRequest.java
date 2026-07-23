package com.cat2bug.system.git.browse.domain;

/**
 * 创建 annotated Git 标签请求。
 */
public class GitTagCreateRequest
{
    private String name;

    /** 标签描述（annotated message），允许空串 */
    private String message;

    private String targetRef;

    private String targetCommitId;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getTargetRef()
    {
        return targetRef;
    }

    public void setTargetRef(String targetRef)
    {
        this.targetRef = targetRef;
    }

    public String getTargetCommitId()
    {
        return targetCommitId;
    }

    public void setTargetCommitId(String targetCommitId)
    {
        this.targetCommitId = targetCommitId;
    }
}
