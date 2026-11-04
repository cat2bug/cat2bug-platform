package com.cat2bug.common.domain.vo;

import com.cat2bug.common.domain.SysDefect;

import java.util.List;

/**
 * 缺陷浅合并请求体。
 */
public class DefectMergeRequest {

    private Long projectId;
    private Long masterDefectId;
    private List<Long> sourceDefectIds;
    private SysDefect defect;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getMasterDefectId() {
        return masterDefectId;
    }

    public void setMasterDefectId(Long masterDefectId) {
        this.masterDefectId = masterDefectId;
    }

    public List<Long> getSourceDefectIds() {
        return sourceDefectIds;
    }

    public void setSourceDefectIds(List<Long> sourceDefectIds) {
        this.sourceDefectIds = sourceDefectIds;
    }

    public SysDefect getDefect() {
        return defect;
    }

    public void setDefect(SysDefect defect) {
        this.defect = defect;
    }
}
