package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户访问令牌 {@code sys_user_access_token}：个人访问令牌与 OAuth 签发的令牌共用。
 */
public class SysUserAccessToken implements Serializable
{
    private static final long serialVersionUID = 1L;

    public static final String SOURCE_MANUAL = "manual";
    public static final String SOURCE_OAUTH = "oauth";
    public static final String SOURCE_OAUTH_REFRESH = "oauth_refresh";

    private Long tokenId;

    private Long userId;

    private String name;

    @JsonIgnore
    private String tokenHash;

    private String tokenPrefix;

    private String source;

    private String clientId;

    /** 列表展示用，来自 sys_oauth_client */
    private String clientName;

    @JsonIgnore
    private String grantId;

    @JsonIgnore
    private String resource;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUsedAt;

    private String lastUsedIp;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date revokedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getTokenId() { return tokenId; }

    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getTokenHash() { return tokenHash; }

    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getTokenPrefix() { return tokenPrefix; }

    public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }

    public String getSource() { return source; }

    public void setSource(String source) { this.source = source; }

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }

    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getGrantId() { return grantId; }

    public void setGrantId(String grantId) { this.grantId = grantId; }

    public String getResource() { return resource; }

    public void setResource(String resource) { this.resource = resource; }

    public Date getExpiresAt() { return expiresAt; }

    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public Date getLastUsedAt() { return lastUsedAt; }

    public void setLastUsedAt(Date lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public String getLastUsedIp() { return lastUsedIp; }

    public void setLastUsedIp(String lastUsedIp) { this.lastUsedIp = lastUsedIp; }

    public Date getRevokedAt() { return revokedAt; }

    public void setRevokedAt(Date revokedAt) { this.revokedAt = revokedAt; }

    public Date getCreateTime() { return createTime; }

    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
