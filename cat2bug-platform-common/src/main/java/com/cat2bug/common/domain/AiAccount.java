package com.cat2bug.common.domain;

/**
 * OpenAI 账号（表 ai_account）。
 */
public class AiAccount {

    private Long accountId;
    private String aiUrl;
    private String modelName;
    private Long maxCompletionTokens;
    private String apiKey;
    private Long createBy;
    private Long projectId;
    private String accountName;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAiUrl() {
        return aiUrl;
    }

    public void setAiUrl(String aiUrl) {
        this.aiUrl = aiUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Long getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(Long maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
