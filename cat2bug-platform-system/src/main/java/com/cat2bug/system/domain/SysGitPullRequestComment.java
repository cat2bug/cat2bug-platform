package com.cat2bug.system.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * Git Pull Request 讨论评论 {@code sys_git_pull_request_comment}
 */
public class SysGitPullRequestComment implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long commentId;

    private Long pullRequestId;

    private Long userId;

    private String body;

    /** 父评论；为空时是顶级评论。 */
    private Long parentId;

    private Date createTime;

    private Date updateTime;

    public Long getCommentId()
    {
        return commentId;
    }

    public void setCommentId(Long commentId)
    {
        this.commentId = commentId;
    }

    public Long getPullRequestId()
    {
        return pullRequestId;
    }

    public void setPullRequestId(Long pullRequestId)
    {
        this.pullRequestId = pullRequestId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
