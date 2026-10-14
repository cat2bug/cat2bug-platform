package com.cat2bug.common.domain;

import java.util.List;

public class SysProject {

    private Long projectId;
    private Long teamId;
    private String teamName;
    private String teamKey;
    private String teamIcon;
    private Integer projectState;
    private String projectName;
    private String projectKey;
    private String projectIcon;
    private String projectIntroduce;
    private boolean collect;
    private String createBy;
    private String createTime;
    private String updateBy;
    private String updateTime;
    private Boolean lock;
    private String lockRemark;
    private List<SysUser> members;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamKey() {
        return teamKey;
    }

    public void setTeamKey(String teamKey) {
        this.teamKey = teamKey;
    }

    public String getTeamIcon() {
        return teamIcon;
    }

    public void setTeamIcon(String teamIcon) {
        this.teamIcon = teamIcon;
    }

    public Integer getProjectState() {
        return projectState;
    }

    public void setProjectState(Integer projectState) {
        this.projectState = projectState;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getProjectIcon() {
        return projectIcon;
    }

    public void setProjectIcon(String projectIcon) {
        this.projectIcon = projectIcon;
    }

    public String getProjectIntroduce() {
        return projectIntroduce;
    }

    public void setProjectIntroduce(String projectIntroduce) {
        this.projectIntroduce = projectIntroduce;
    }

    public boolean isCollect() {
        return collect;
    }

    public void setCollect(boolean collect) {
        this.collect = collect;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public Boolean getLock() {
        return lock;
    }

    public void setLock(Boolean lock) {
        this.lock = lock;
    }

    public String getLockRemark() {
        return lockRemark;
    }

    public void setLockRemark(String lockRemark) {
        this.lockRemark = lockRemark;
    }

    public List<SysUser> getMembers() {
        return members;
    }

    public void setMembers(List<SysUser> members) {
        this.members = members;
    }
}
