package com.cat2bug.common.domain;

import java.util.Date;

/** 私有端出站云同步任务。 */
public class SysCloudPushJob {

    private String jobId;
    private Long projectId;
    private String pullKey;
    private String state;
    private Integer percent;
    private String phase;
    private String errorMessage;
    private String message;
    private String sessionId;
    private String selectionJson;
    private String logsJson;
    private Long packedBytes;
    private Long maxTotalBytes;
    private Integer legacyFull;
    private Date createTime;
    private Date updateTime;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getPullKey() {
        return pullKey;
    }

    public void setPullKey(String pullKey) {
        this.pullKey = pullKey;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getPercent() {
        return percent;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSelectionJson() {
        return selectionJson;
    }

    public void setSelectionJson(String selectionJson) {
        this.selectionJson = selectionJson;
    }

    public String getLogsJson() {
        return logsJson;
    }

    public void setLogsJson(String logsJson) {
        this.logsJson = logsJson;
    }

    public Long getPackedBytes() {
        return packedBytes;
    }

    public void setPackedBytes(Long packedBytes) {
        this.packedBytes = packedBytes;
    }

    public Long getMaxTotalBytes() {
        return maxTotalBytes;
    }

    public void setMaxTotalBytes(Long maxTotalBytes) {
        this.maxTotalBytes = maxTotalBytes;
    }

    public Integer getLegacyFull() {
        return legacyFull;
    }

    public void setLegacyFull(Integer legacyFull) {
        this.legacyFull = legacyFull;
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
