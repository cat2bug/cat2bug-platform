package com.cat2bug.common.domain;

import java.util.Date;

/** 新建项目复制任务 sys_project_copy_job。 */
public class ProjectCopyJob {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String PARTIAL = "PARTIAL";

    private String jobId;
    private Long sourceProjectId;
    private Long targetProjectId;
    private String state;
    private Integer percent;
    private String selectionJson;
    private String itemsJson;
    private Long estimatedBytes;
    private Long actualBytes;
    private String errorMessage;
    private String createBy;
    private Date createTime;
    private Date updateTime;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Long getSourceProjectId() {
        return sourceProjectId;
    }

    public void setSourceProjectId(Long sourceProjectId) {
        this.sourceProjectId = sourceProjectId;
    }

    public Long getTargetProjectId() {
        return targetProjectId;
    }

    public void setTargetProjectId(Long targetProjectId) {
        this.targetProjectId = targetProjectId;
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

    public String getSelectionJson() {
        return selectionJson;
    }

    public void setSelectionJson(String selectionJson) {
        this.selectionJson = selectionJson;
    }

    public String getItemsJson() {
        return itemsJson;
    }

    public void setItemsJson(String itemsJson) {
        this.itemsJson = itemsJson;
    }

    public Long getEstimatedBytes() {
        return estimatedBytes;
    }

    public void setEstimatedBytes(Long estimatedBytes) {
        this.estimatedBytes = estimatedBytes;
    }

    public Long getActualBytes() {
        return actualBytes;
    }

    public void setActualBytes(Long actualBytes) {
        this.actualBytes = actualBytes;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
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
