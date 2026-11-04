package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

public class SysCaseReview {

    private Long reviewId;
    private Long projectId;
    private String status;
    private String reportMarkdown;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String completeBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completeTime;
    private List<Long> caseIds;
    private List<SysCaseReviewItem> items;
    /** 本次完成时因已在评审中而跳过的用例编号，不入库。 */
    private List<String> skippedCaseNums;

    /** 列表摘要：评审条目总数（查询时 SQL 聚合，不入库）。 */
    private Integer itemCount;
    /** 列表摘要：结论为 passed 的条数。 */
    private Integer passedCount;
    /** 列表摘要：结论为 changes 的条数。 */
    private Integer changesCount;
    /** 列表摘要：结论为 pending 的条数。 */
    private Integer pendingCount;

    /** 列表筛选：发起时间起（仅查询，不入库）。 */
    private String createTimeBegin;
    /** 列表筛选：发起时间止（仅查询，不入库）。 */
    private String createTimeEnd;

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReportMarkdown() {
        return reportMarkdown;
    }

    public void setReportMarkdown(String reportMarkdown) {
        this.reportMarkdown = reportMarkdown;
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

    public String getCompleteBy() {
        return completeBy;
    }

    public void setCompleteBy(String completeBy) {
        this.completeBy = completeBy;
    }

    public Date getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(Date completeTime) {
        this.completeTime = completeTime;
    }

    public List<Long> getCaseIds() {
        return caseIds;
    }

    public void setCaseIds(List<Long> caseIds) {
        this.caseIds = caseIds;
    }

    public List<SysCaseReviewItem> getItems() {
        return items;
    }

    public void setItems(List<SysCaseReviewItem> items) {
        this.items = items;
    }

    public List<String> getSkippedCaseNums() {
        return skippedCaseNums;
    }

    public void setSkippedCaseNums(List<String> skippedCaseNums) {
        this.skippedCaseNums = skippedCaseNums;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Integer getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(Integer passedCount) {
        this.passedCount = passedCount;
    }

    public Integer getChangesCount() {
        return changesCount;
    }

    public void setChangesCount(Integer changesCount) {
        this.changesCount = changesCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public String getCreateTimeBegin() {
        return createTimeBegin;
    }

    public void setCreateTimeBegin(String createTimeBegin) {
        this.createTimeBegin = createTimeBegin;
    }

    public String getCreateTimeEnd() {
        return createTimeEnd;
    }

    public void setCreateTimeEnd(String createTimeEnd) {
        this.createTimeEnd = createTimeEnd;
    }
}
