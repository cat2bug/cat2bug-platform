package com.cat2bug.system.domain.git;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * Webhook API 响应（不含 secret 明文）
 */
public class SysGitRepositoryWebhookVo
{
    private Long webhookId;

    private Long repositoryId;

    private String url;

    private String alias;

    private List<String> events;

    private Boolean active;

    private Boolean secretConfigured;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getWebhookId()
    {
        return webhookId;
    }

    public void setWebhookId(Long webhookId)
    {
        this.webhookId = webhookId;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public List<String> getEvents()
    {
        return events;
    }

    public void setEvents(List<String> events)
    {
        this.events = events;
    }

    public Boolean getActive()
    {
        return active;
    }

    public void setActive(Boolean active)
    {
        this.active = active;
    }

    public Boolean getSecretConfigured()
    {
        return secretConfigured;
    }

    public void setSecretConfigured(Boolean secretConfigured)
    {
        this.secretConfigured = secretConfigured;
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
