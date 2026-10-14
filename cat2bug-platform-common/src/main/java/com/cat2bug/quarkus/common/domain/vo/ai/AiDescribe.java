package com.cat2bug.quarkus.common.domain.vo.ai;

public class AiDescribe {

    private String describe;
    private Long projectId;
    /** ollama / openai，缺省 ollama */
    private String serviceType;
    /** Ollama：模型名；OpenAI：账号 ID（字符串） */
    private String modelId;

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
