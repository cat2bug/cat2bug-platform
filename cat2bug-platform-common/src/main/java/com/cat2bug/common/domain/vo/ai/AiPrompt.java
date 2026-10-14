package com.cat2bug.common.domain.vo.ai;

public class AiPrompt {

    private String serviceType;
    private String modelId;
    private String prompt;
    private int rowCount;
    private long[] context;

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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public long[] getContext() {
        return context;
    }

    public void setContext(long[] context) {
        this.context = context;
    }
}
