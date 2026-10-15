package com.cat2bug.common.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * OAuth 签发的令牌 {@code sys_user_access_token}：访问令牌与刷新令牌，均绑定授权时选择的项目。
 */
public class SysUserAccessToken implements Serializable
{
    private static final long serialVersionUID = 1L;

    public static final String SOURCE_OAUTH = "oauth";
    public static final String SOURCE_OAUTH_REFRESH = "oauth_refresh";

    private Long tokenId;

    private Long userId;

    private Long projectId;

    private String name;

    private String tokenHash;

    private String tokenPrefix;

    private String source;

    private String clientId;

    private String grantId;

    private String resource;

    private Date expiresAt;

    private Date lastUsedAt;

    private String lastUsedIp;

    private Date revokedAt;

    private Date createTime;

    public Long getTokenId() { return tokenId; }

    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProjectId() { return projectId; }

    public void setProjectId(Long projectId) { this.projectId = projectId; }

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
