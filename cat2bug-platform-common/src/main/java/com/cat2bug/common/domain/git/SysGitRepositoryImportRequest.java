package com.cat2bug.common.domain.git;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 从远端 URL 一次性导入仓库的请求体。
 * <p>
 * {@code token} / {@code username} 仅用于当次 clone，不落库；{@link #toString()} 刻意不输出凭据，
 * 避免操作日志 / 异常拼接泄漏。
 */
public class SysGitRepositoryImportRequest
{
    private Long projectId;

    private String name;

    private String slug;

    /** {@code public} / {@code project} */
    private String visibility;

    private String defaultBranch;

    private String remark;

    /** 远端 HTTPS/HTTP clone URL */
    private String sourceUrl;

    /** 可选；为空且 token 非空时服务端默认 oauth2 */
    private String username;

    /** 可选 PAT / 密码；请求结束后丢弃，永不持久化 */
    private String token;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSlug()
    {
        return slug;
    }

    public void setSlug(String slug)
    {
        this.slug = slug;
    }

    public String getVisibility()
    {
        return visibility;
    }

    public void setVisibility(String visibility)
    {
        this.visibility = visibility;
    }

    public String getDefaultBranch()
    {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch)
    {
        this.defaultBranch = defaultBranch;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getSourceUrl()
    {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl)
    {
        this.sourceUrl = sourceUrl;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("projectId", getProjectId())
            .append("name", getName())
            .append("slug", getSlug())
            .append("visibility", getVisibility())
            .append("defaultBranch", getDefaultBranch())
            .append("remark", getRemark())
            .append("sourceUrl", getSourceUrl())
            .append("username", getUsername() == null ? null : "***")
            .append("token", getToken() == null ? null : "***")
            .toString();
    }
}
