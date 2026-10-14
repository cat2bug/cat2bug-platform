package com.cat2bug.quarkus.common.domain;

import com.cat2bug.quarkus.common.domain.BaseEntity;

import java.util.Date;

/**
 * Git Pull Request {@code sys_git_pull_request}
 */
public class SysGitPullRequest extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long pullRequestId;

    private Long repositoryId;

    private Integer number;

    private String title;

    private String body;

    private String sourceBranch;

    private String targetBranch;

    /** 创建时的源分支提交快照。 */
    private String sourceSha;

    /** 创建或合并时的目标分支提交快照。 */
    private String targetSha;

    /** open、merged 或 closed。 */
    private String state;

    private Long authorId;

    private Long mergedBy;

    private Date mergedAt;

    private Date closedAt;

    public Long getPullRequestId()
    {
        return pullRequestId;
    }

    public void setPullRequestId(Long pullRequestId)
    {
        this.pullRequestId = pullRequestId;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public Integer getNumber()
    {
        return number;
    }

    public void setNumber(Integer number)
    {
        this.number = number;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public String getSourceBranch()
    {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch)
    {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch()
    {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch)
    {
        this.targetBranch = targetBranch;
    }

    public String getSourceSha()
    {
        return sourceSha;
    }

    public void setSourceSha(String sourceSha)
    {
        this.sourceSha = sourceSha;
    }

    public String getTargetSha()
    {
        return targetSha;
    }

    public void setTargetSha(String targetSha)
    {
        this.targetSha = targetSha;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public Long getAuthorId()
    {
        return authorId;
    }

    public void setAuthorId(Long authorId)
    {
        this.authorId = authorId;
    }

    public Long getMergedBy()
    {
        return mergedBy;
    }

    public void setMergedBy(Long mergedBy)
    {
        this.mergedBy = mergedBy;
    }

    public Date getMergedAt()
    {
        return mergedAt;
    }

    public void setMergedAt(Date mergedAt)
    {
        this.mergedAt = mergedAt;
    }

    public Date getClosedAt()
    {
        return closedAt;
    }

    public void setClosedAt(Date closedAt)
    {
        this.closedAt = closedAt;
    }
}
