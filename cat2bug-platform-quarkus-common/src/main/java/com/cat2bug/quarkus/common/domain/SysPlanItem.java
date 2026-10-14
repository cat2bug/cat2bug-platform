package com.cat2bug.quarkus.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SysPlanItem {

    public static final String PLAN_ITEM_DEFAULT_STATE = "unexecuted";

    private String planItemId;
    private String planId;
    private Long moduleId;
    private Long caseId;
    private String planItemState;
    private Long updateById;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private List<Long> defectIds;
    private Map<String, Object> params = new HashMap<>();

    private String caseName;
    private String moduleName;
    private Long caseType;
    private String caseExpect;
    private List<SysCaseStep> caseStep;
    private Long caseLevel;
    private String casePreconditions;
    private Long caseNum;
    private Long projectId;
    private String remark;
    private String imgUrls;
    private String annexUrls;
    private String caseData;

    public String getPlanItemId() {
        return planItemId;
    }

    public void setPlanItemId(String planItemId) {
        this.planItemId = planItemId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getPlanItemState() {
        return planItemState;
    }

    public void setPlanItemState(String planItemState) {
        this.planItemState = planItemState;
    }

    public Long getUpdateById() {
        return updateById;
    }

    public void setUpdateById(Long updateById) {
        this.updateById = updateById;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<Long> getDefectIds() {
        return defectIds;
    }

    public void setDefectIds(List<Long> defectIds) {
        this.defectIds = defectIds;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params != null ? params : new HashMap<>();
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public Long getCaseType() {
        return caseType;
    }

    public void setCaseType(Long caseType) {
        this.caseType = caseType;
    }

    public String getCaseExpect() {
        return caseExpect;
    }

    public void setCaseExpect(String caseExpect) {
        this.caseExpect = caseExpect;
    }

    public List<SysCaseStep> getCaseStep() {
        return caseStep;
    }

    public void setCaseStep(List<SysCaseStep> caseStep) {
        this.caseStep = caseStep;
    }

    public Long getCaseLevel() {
        return caseLevel;
    }

    public void setCaseLevel(Long caseLevel) {
        this.caseLevel = caseLevel;
    }

    public String getCasePreconditions() {
        return casePreconditions;
    }

    public void setCasePreconditions(String casePreconditions) {
        this.casePreconditions = casePreconditions;
    }

    public Long getCaseNum() {
        return caseNum;
    }

    public void setCaseNum(Long caseNum) {
        this.caseNum = caseNum;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getImgUrls() {
        return imgUrls;
    }

    public void setImgUrls(String imgUrls) {
        this.imgUrls = imgUrls;
    }

    public String getAnnexUrls() {
        return annexUrls;
    }

    public void setAnnexUrls(String annexUrls) {
        this.annexUrls = annexUrls;
    }

    public String getCaseData() {
        return caseData;
    }

    public void setCaseData(String caseData) {
        this.caseData = caseData;
    }
}
