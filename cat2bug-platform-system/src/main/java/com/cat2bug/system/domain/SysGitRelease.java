package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.List;

/**
 * Git 发行版 {@code sys_git_release}
 */
public class SysGitRelease extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long releaseId;

    private Long repositoryId;

    private String tagName;

    private String targetCommitId;

    private String title;

    private String body;

    /** 0=已发布可见策略下的非草稿，1=草稿 */
    private Integer draft;

    /** 0=正式，1=预发布 */
    private Integer prerelease;

    private Long publisherId;

    private String publisherName;

    private Date publishedAt;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<SysGitReleaseAsset> assets;

    /** 创建发行版时可选：解析目标 commit 的 ref（非持久化） */
    private String targetRef;

    /** 目标 commit 短消息（非持久化，详情接口填充） */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String targetCommitMessage;

    public Long getReleaseId()
    {
        return releaseId;
    }

    public void setReleaseId(Long releaseId)
    {
        this.releaseId = releaseId;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public String getTargetCommitId()
    {
        return targetCommitId;
    }

    public void setTargetCommitId(String targetCommitId)
    {
        this.targetCommitId = targetCommitId;
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

    public Integer getDraft()
    {
        return draft;
    }

    public void setDraft(Integer draft)
    {
        this.draft = draft;
    }

    public boolean isDraftRelease()
    {
        return draft != null && draft == 1;
    }

    public Integer getPrerelease()
    {
        return prerelease;
    }

    public void setPrerelease(Integer prerelease)
    {
        this.prerelease = prerelease;
    }

    public Long getPublisherId()
    {
        return publisherId;
    }

    public void setPublisherId(Long publisherId)
    {
        this.publisherId = publisherId;
    }

    public String getPublisherName()
    {
        return publisherName;
    }

    public void setPublisherName(String publisherName)
    {
        this.publisherName = publisherName;
    }

    public Date getPublishedAt()
    {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt)
    {
        this.publishedAt = publishedAt;
    }

    public List<SysGitReleaseAsset> getAssets()
    {
        return assets;
    }

    public void setAssets(List<SysGitReleaseAsset> assets)
    {
        this.assets = assets;
    }

    public String getTargetRef()
    {
        return targetRef;
    }

    public void setTargetRef(String targetRef)
    {
        this.targetRef = targetRef;
    }

    public String getTargetCommitMessage()
    {
        return targetCommitMessage;
    }

    public void setTargetCommitMessage(String targetCommitMessage)
    {
        this.targetCommitMessage = targetCommitMessage;
    }
}
