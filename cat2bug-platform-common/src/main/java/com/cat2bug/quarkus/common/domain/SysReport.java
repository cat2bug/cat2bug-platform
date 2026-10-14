package com.cat2bug.quarkus.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

public class SysReport {

    public static final String KEY = "report";

    private Long reportId;
    private String reportTitle;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reportTime;
    private String reportDescription;
    private String reportDataCoder;
    private Object reportData;
    private String reportSource;
    private Long createById;
    private String createBy;
    private String createByAvatar;
    private List<Long> createByIds;
    private Long projectId;
    private String reportKey;
    private List<SysUser> focusList;

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public Date getReportTime() {
        return reportTime;
    }

    public void setReportTime(Date reportTime) {
        this.reportTime = reportTime;
    }

    public String getReportDescription() {
        return reportDescription;
    }

    public void setReportDescription(String reportDescription) {
        this.reportDescription = reportDescription;
    }

    public String getReportDataCoder() {
        return reportDataCoder;
    }

    public void setReportDataCoder(String reportDataCoder) {
        this.reportDataCoder = reportDataCoder;
    }

    public Object getReportData() {
        return reportData;
    }

    public void setReportData(Object reportData) {
        this.reportData = reportData;
    }

    public String getReportSource() {
        return reportSource;
    }

    public void setReportSource(String reportSource) {
        this.reportSource = reportSource;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getCreateByAvatar() {
        return createByAvatar;
    }

    public void setCreateByAvatar(String createByAvatar) {
        this.createByAvatar = createByAvatar;
    }

    public List<Long> getCreateByIds() {
        return createByIds;
    }

    public void setCreateByIds(List<Long> createByIds) {
        this.createByIds = createByIds;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getReportKey() {
        return reportKey;
    }

    public void setReportKey(String reportKey) {
        this.reportKey = reportKey;
    }

    public List<SysUser> getFocusList() {
        return focusList;
    }

    public void setFocusList(List<SysUser> focusList) {
        this.focusList = focusList;
    }
}
