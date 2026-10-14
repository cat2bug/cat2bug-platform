package com.cat2bug.quarkus.common.domain.im;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IMUserConfig {

    private Long imConfigId;

    @JsonProperty("memberId")
    private Long userId;

    private Long projectId;
    private String groupName;
    private IMConfig config;
    private String updateTime;

    public Long getImConfigId() {
        return imConfigId;
    }

    public void setImConfigId(Long imConfigId) {
        this.imConfigId = imConfigId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public IMConfig getConfig() {
        return config;
    }

    public void setConfig(IMConfig config) {
        this.config = config;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
