package com.cat2bug.common.domain.open;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * Open API 缺陷报告推送请求，对齐 Spring {@code SysReport<List<ApiDefectRequest>>}。
 */
public class OpenApiDefectReportRequest {

    private Long reportId;
    private String reportTitle;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reportTime;
    private String reportDescription;
    private String reportDataCoder;
    private List<ApiDefectRequest> reportData;
    private String reportSource;
    private String reportKey;
    private String handler;

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

    public List<ApiDefectRequest> getReportData() {
        return reportData;
    }

    public void setReportData(List<ApiDefectRequest> reportData) {
        this.reportData = reportData;
    }

    public String getReportSource() {
        return reportSource;
    }

    public void setReportSource(String reportSource) {
        this.reportSource = reportSource;
    }

    public String getReportKey() {
        return reportKey;
    }

    public void setReportKey(String reportKey) {
        this.reportKey = reportKey;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }
}
