package com.cat2bug.quarkus.common.domain;

public class SysUserProject {

    private Long userProjectId;
    private Long userId;
    private Long projectId;
    private Integer projectLock;
    private Boolean collect;

    public Long getUserProjectId() {
        return userProjectId;
    }

    public void setUserProjectId(Long userProjectId) {
        this.userProjectId = userProjectId;
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

    public Integer getProjectLock() {
        return projectLock;
    }

    public void setProjectLock(Integer projectLock) {
        this.projectLock = projectLock;
    }

    public Boolean getCollect() {
        return collect;
    }

    public void setCollect(Boolean collect) {
        this.collect = collect;
    }
}
