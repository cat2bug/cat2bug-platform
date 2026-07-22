package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 仓库出站 Webhook {@code sys_git_repository_webhook}
 */
public class SysGitRepositoryWebhook extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long webhookId;

    private Long repositoryId;

    private String url;

    private String alias;

    /** AES 加密后的 secret，不可逆展示 */
    private String secretCipher;

    /** JSON 数组：push、tag、pull_request */
    private String events;

    /** 1=激活，0=停用 */
    private Integer active;

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

    public String getSecretCipher()
    {
        return secretCipher;
    }

    public void setSecretCipher(String secretCipher)
    {
        this.secretCipher = secretCipher;
    }

    public String getEvents()
    {
        return events;
    }

    public void setEvents(String events)
    {
        this.events = events;
    }

    public Integer getActive()
    {
        return active;
    }

    public void setActive(Integer active)
    {
        this.active = active;
    }

    public boolean isActiveFlag()
    {
        return active != null && active == 1;
    }
}
