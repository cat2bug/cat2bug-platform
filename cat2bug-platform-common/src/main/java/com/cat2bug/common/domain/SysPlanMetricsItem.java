package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/**
 * 测试计划质量指标（雷达图）
 */
public class SysPlanMetricsItem {

    private String planId;
    private String planName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private double discovery;
    private double repair;
    private double detection;
    private double severity;
    private double restart;
    private double escape;

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public double getDiscovery() {
        return discovery;
    }

    public void setDiscovery(double discovery) {
        this.discovery = discovery;
    }

    public double getRepair() {
        return repair;
    }

    public void setRepair(double repair) {
        this.repair = repair;
    }

    public double getDetection() {
        return detection;
    }

    public void setDetection(double detection) {
        this.detection = detection;
    }

    public double getSeverity() {
        return severity;
    }

    public void setSeverity(double severity) {
        this.severity = severity;
    }

    public double getRestart() {
        return restart;
    }

    public void setRestart(double restart) {
        this.restart = restart;
    }

    public double getEscape() {
        return escape;
    }

    public void setEscape(double escape) {
        this.escape = escape;
    }
}
