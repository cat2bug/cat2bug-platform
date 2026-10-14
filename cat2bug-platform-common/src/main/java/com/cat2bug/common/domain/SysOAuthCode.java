package com.cat2bug.common.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * OAuth 授权码 {@code sys_oauth_code}，只存哈希，一次性。
 */
public class SysOAuthCode implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String codeHash;

    private String clientId;

    private Long userId;

    private String redirectUri;

    private String codeChallenge;

    private String resource;

    private Date expiresAt;

    private Integer used;

    private Date createTime;

    public String getCodeHash() { return codeHash; }

    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public String getClientId() { return clientId; }

    public void setClientId(String clientId) { this.clientId = clientId; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public String getRedirectUri() { return redirectUri; }

    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getCodeChallenge() { return codeChallenge; }

    public void setCodeChallenge(String codeChallenge) { this.codeChallenge = codeChallenge; }

    public String getResource() { return resource; }

    public void setResource(String resource) { this.resource = resource; }

    public Date getExpiresAt() { return expiresAt; }

    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public Integer getUsed() { return used; }

    public void setUsed(Integer used) { this.used = used; }

    public Date getCreateTime() { return createTime; }

    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
