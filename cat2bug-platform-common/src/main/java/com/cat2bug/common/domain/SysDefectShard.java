package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class SysDefectShard {

    private String defectShardId;
    private Long projectId;
    private Long defectId;
    private String password;
    private Long createById;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date agingTime;
    private Integer agingHour;
    private String defaultLang;

    public String getDefectShardId() {
        return defectShardId;
    }

    public void setDefectShardId(String defectShardId) {
        this.defectShardId = defectShardId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getDefectId() {
        return defectId;
    }

    public void setDefectId(Long defectId) {
        this.defectId = defectId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
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

    public Date getAgingTime() {
        return agingTime;
    }

    public void setAgingTime(Date agingTime) {
        this.agingTime = agingTime;
    }

    public Integer getAgingHour() {
        return agingHour;
    }

    public void setAgingHour(Integer agingHour) {
        this.agingHour = agingHour;
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public void setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
    }
}
