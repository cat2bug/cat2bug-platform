package com.cat2bug.quarkus.common.domain.vo.ai;

public class AiSysPrompt extends AiPrompt {

    private Long projectId;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
