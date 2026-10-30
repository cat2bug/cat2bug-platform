package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/** 缺陷计划依赖边 sys_defect_plan_link（FS：完成→开始）。 */
public class SysDefectPlanLink {

    private Long linkId;
    private Long projectId;
    private Long fromDefectId;
    private Long toDefectId;
    /** 目前仅 FS */
    private String linkType;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getFromDefectId() {
        return fromDefectId;
    }

    public void setFromDefectId(Long fromDefectId) {
        this.fromDefectId = fromDefectId;
    }

    public Long getToDefectId() {
        return toDefectId;
    }

    public void setToDefectId(Long toDefectId) {
        this.toDefectId = toDefectId;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
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
}
