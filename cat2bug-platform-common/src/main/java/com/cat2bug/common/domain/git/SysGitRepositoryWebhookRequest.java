package com.cat2bug.common.domain.git;

import java.util.List;

/**
 * 创建/更新 Webhook 请求体
 */
public class SysGitRepositoryWebhookRequest
{
    private String url;

    private String alias;

    /** 创建时可选；留空则服务端生成 */
    private String secret;

    private List<String> events;

    private Boolean active;

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

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
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
}
