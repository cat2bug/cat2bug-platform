package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 项目工作台自定义页签（私有 / 项目共享）。
 * 固定「工作台」仍走 {@link SysUserWorkbench}。
 */
public class SysProjectWorkbenchTab {

    public static final String VISIBILITY_PRIVATE = "private";
    public static final String VISIBILITY_PROJECT = "project";
    public static final int MAX_SHARED_PER_PROJECT = 24;
    public static final int MAX_NAME_LENGTH = 12;

    private Long tabId;
    private Long projectId;
    private Long ownerUserId;
    /** private | project */
    private String visibility;
    private String tabName;
    private String preset;
    private String configJson;
    private Long tabSort;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getTabId() {
        return tabId;
    }

    public void setTabId(Long tabId) {
        this.tabId = tabId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public String getPreset() {
        return preset;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Long getTabSort() {
        return tabSort;
    }

    public void setTabSort(Long tabSort) {
        this.tabSort = tabSort;
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
