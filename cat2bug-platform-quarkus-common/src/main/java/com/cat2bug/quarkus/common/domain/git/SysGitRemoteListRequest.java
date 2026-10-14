package com.cat2bug.quarkus.common.domain.git;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 按平台 PAT 列远端仓库请求体。凭据不落库；{@link #toString()} 不输出 token。
 */
public class SysGitRemoteListRequest
{
    /** {@code github} | {@code gitlab} | {@code gitee} */
    private String provider;

    /** 个人访问令牌（必填） */
    private String token;

    /** 仅 GitLab：主机基址，默认 {@code https://gitlab.com} */
    private String host;

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("provider", getProvider())
            .append("token", getToken() == null ? null : "***")
            .append("host", getHost())
            .toString();
    }
}
