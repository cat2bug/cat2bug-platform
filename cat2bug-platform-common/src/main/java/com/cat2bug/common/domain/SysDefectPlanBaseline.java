package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 项目活跃计划基线 sys_defect_plan_baseline。 */
public class SysDefectPlanBaseline {

    private Long baselineId;
    private Long projectId;
    private String frozenBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date frozenTime;
    private List<SysDefectPlanBaselineItem> items = new ArrayList<>();

    public Long getBaselineId() {
        return baselineId;
    }

    public void setBaselineId(Long baselineId) {
        this.baselineId = baselineId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getFrozenBy() {
        return frozenBy;
    }

    public void setFrozenBy(String frozenBy) {
        this.frozenBy = frozenBy;
    }

    public Date getFrozenTime() {
        return frozenTime;
    }

    public void setFrozenTime(Date frozenTime) {
        this.frozenTime = frozenTime;
    }

    public List<SysDefectPlanBaselineItem> getItems() {
        return items;
    }

    public void setItems(List<SysDefectPlanBaselineItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
