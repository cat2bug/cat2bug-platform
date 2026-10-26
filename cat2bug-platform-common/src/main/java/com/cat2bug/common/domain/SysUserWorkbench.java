package com.cat2bug.common.domain;

import java.util.Date;

public class SysUserWorkbench {

    private Long workbenchId;
    private Long userId;
    private Long projectId;
    private String configJson;
    private Date createTime;
    private Date updateTime;

    public Long getWorkbenchId() {
        return workbenchId;
    }

    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
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

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
