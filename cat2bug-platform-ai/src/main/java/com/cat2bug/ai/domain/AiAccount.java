package com.cat2bug.ai.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * OpenAI账号对象 ai_account
 * 
 * @author yuzhantao
 * @date 2025-12-28
 */
public class AiAccount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 账号ID */
    private Long accountId;

    /** AI服务网址 */
    @Excel(name = "AI服务网址")
    private String aiUrl;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 最大Token */
    @Excel(name = "最大Token")
    private Long maxCompletionTokens;

    /** 密钥 */
    @Excel(name = "密钥")
    private String apiKey;

    /** 关联项目ID */
    private Long projectId;

    /** 账号名称 */
    @Excel(name = "账号名称")
    private String accountName;

    public void setAccountId(Long accountId) 
    {
        this.accountId = accountId;
    }

    public Long getAccountId() 
    {
        return accountId;
    }
    public void setAiUrl(String aiUrl) 
    {
        this.aiUrl = aiUrl;
    }

    public String getAiUrl() 
    {
        return aiUrl;
    }
    public void setModelName(String modelName) 
    {
        this.modelName = modelName;
    }

    public String getModelName() 
    {
        return modelName;
    }
    public void setMaxCompletionTokens(Long maxCompletionTokens) 
    {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public Long getMaxCompletionTokens() 
    {
        return maxCompletionTokens;
    }
    public void setApiKey(String apiKey) 
    {
        this.apiKey = apiKey;
    }

    public String getApiKey() 
    {
        return apiKey;
    }
    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }
    public void setAccountName(String accountName) 
    {
        this.accountName = accountName;
    }

    public String getAccountName() 
    {
        return accountName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("accountId", getAccountId())
            .append("aiUrl", getAiUrl())
            .append("modelName", getModelName())
            .append("maxCompletionTokens", getMaxCompletionTokens())
            .append("apiKey", getApiKey())
            .append("createBy", getCreateBy())
            .append("projectId", getProjectId())
            .append("accountName", getAccountName())
            .toString();
    }
}
