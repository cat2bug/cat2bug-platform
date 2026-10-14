package com.cat2bug.quarkus.common.domain;

public class SysReportHistory extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long historyId;
    private Long reportId;
    /** 保存时的正文快照。 */
    private String reportDescription;
    private SysUser createMember;
    /** 列表展示用，不持久化。 */
    private Integer commentCount;

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportDescription() {
        return reportDescription;
    }

    public void setReportDescription(String reportDescription) {
        this.reportDescription = reportDescription;
    }

    public SysUser getCreateMember() {
        return createMember;
    }

    public void setCreateMember(SysUser createMember) {
        this.createMember = createMember;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }
}
