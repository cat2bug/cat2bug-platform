package com.cat2bug.common.domain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * OAuth 动态注册的公开客户端 {@code sys_oauth_client}。
 */
public class SysOAuthClient implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String clientId;

    private String clientName;

    /** 换行分隔 */
    private String redirectUris;

    private Date createTime;

    private Date lastUsedAt;

    public List<String> redirectUriList()
    {
        if (redirectUris == null || redirectUris.isBlank()) {
            return List.of();
        }
        return Arrays.stream(redirectUris.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }

    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getRedirectUris() { return redirectUris; }

    public void setRedirectUris(String redirectUris) { this.redirectUris = redirectUris; }

    public Date getCreateTime() { return createTime; }

    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getLastUsedAt() { return lastUsedAt; }

    public void setLastUsedAt(Date lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
