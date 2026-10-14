package com.cat2bug.quarkus.common.domain;

public class SysUserConfig {

    private Long userConfigId;
    private Long currentTeamId;
    private String currentTeamIcon;
    private String currentTeamName;
    private Long currentProjectId;
    private String currentProjectName;
    private Long userId;
    private String lifeContent;
    private boolean currentTeamLock;
    private String currentTeamLockRemark;
    private boolean currentProjectLock;
    private String currentProjectLockRemark;

    public Long getUserConfigId() {
        return userConfigId;
    }

    public void setUserConfigId(Long userConfigId) {
        this.userConfigId = userConfigId;
    }

    public Long getCurrentTeamId() {
        return currentTeamId;
    }

    public void setCurrentTeamId(Long currentTeamId) {
        this.currentTeamId = currentTeamId;
    }

    public String getCurrentTeamIcon() {
        return currentTeamIcon;
    }

    public void setCurrentTeamIcon(String currentTeamIcon) {
        this.currentTeamIcon = currentTeamIcon;
    }

    public String getCurrentTeamName() {
        return currentTeamName;
    }

    public void setCurrentTeamName(String currentTeamName) {
        this.currentTeamName = currentTeamName;
    }

    public Long getCurrentProjectId() {
        return currentProjectId;
    }

    public void setCurrentProjectId(Long currentProjectId) {
        this.currentProjectId = currentProjectId;
    }

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    public void setCurrentProjectName(String currentProjectName) {
        this.currentProjectName = currentProjectName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLifeContent() {
        return lifeContent;
    }

    public void setLifeContent(String lifeContent) {
        this.lifeContent = lifeContent;
    }

    public boolean isCurrentTeamLock() {
        return currentTeamLock;
    }

    public void setCurrentTeamLock(boolean currentTeamLock) {
        this.currentTeamLock = currentTeamLock;
    }

    public String getCurrentTeamLockRemark() {
        return currentTeamLockRemark;
    }

    public void setCurrentTeamLockRemark(String currentTeamLockRemark) {
        this.currentTeamLockRemark = currentTeamLockRemark;
    }

    public boolean isCurrentProjectLock() {
        return currentProjectLock;
    }

    public void setCurrentProjectLock(boolean currentProjectLock) {
        this.currentProjectLock = currentProjectLock;
    }

    public void setCurrentProjectLockRemark(String currentProjectLockRemark) {
        this.currentProjectLockRemark = currentProjectLockRemark;
    }

    public String getCurrentProjectLockRemark() {
        return currentProjectLockRemark;
    }
}
