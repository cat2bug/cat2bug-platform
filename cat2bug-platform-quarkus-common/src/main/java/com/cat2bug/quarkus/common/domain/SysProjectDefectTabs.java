package com.cat2bug.quarkus.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class SysProjectDefectTabs {

    private Long tabId;
    private String tabName;
    private Long projectId;
    private Long userId;
    private SysDefect config;
    private Long tabSort;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getTabId() {
        return tabId;
    }

    public void setTabId(Long tabId) {
        this.tabId = tabId;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SysDefect getConfig() {
        return config;
    }

    public void setConfig(SysDefect config) {
        this.config = config;
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
}
