package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class SysCase {

    private Long caseId;
    private String caseName;
    private Long moduleId;
    private String moduleName;
    private Long caseType;
    private String caseTypeName;
    /** 为 true 时更新类型，允许把类型清空为 null */
    private Boolean updateCaseType;
    /** null 未评审，in_review 评审中，passed 通过，changes 需修改 */
    private String reviewStatus;
    private String caseExpect;
    private List<SysCaseStep> caseStep;
    private Long caseLevel;
    private String caseLevelName;
    private String casePreconditions;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private Long caseNum;
    private Long projectId;
    private String remark;
    private String imgUrls;
    private String annexUrls;
    private String caseData;
    private Integer defectProcessingCount;
    private Integer defectAuditCount;
    private Integer defectCloseCount;

    /** 导入：步骤脚本（行内 step---expect） */
    private String caseStepScript;

    /** 导入：图片对象 JSON */
    private String imgObjects;

    /** 导出：更新时间列（从 updateTime 复制） */
    @JsonIgnore
    private Date caseExportUpdateTime;

    /** 导入校验：行号等临时参数 */
    private Map<String, Object> params;

    /** 计划执行：关联的计划子项 */
    private SysPlanItem planItem;

    /** 当前正在编辑此用例的成员（非持久化） */
    private List<SysUser> focusList;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
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

    public String getCaseTypeName() {
        return caseTypeName;
    }

    public void setCaseTypeName(String caseTypeName) {
        this.caseTypeName = caseTypeName;
    }

    public Boolean getUpdateCaseType() {
        return updateCaseType;
    }

    public void setUpdateCaseType(Boolean updateCaseType) {
        this.updateCaseType = updateCaseType;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
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

    public String getCaseLevelName() {
        return caseLevelName;
    }

    public void setCaseLevelName(String caseLevelName) {
        this.caseLevelName = caseLevelName;
    }

    public String getCasePreconditions() {
        return casePreconditions;
    }

    public void setCasePreconditions(String casePreconditions) {
        this.casePreconditions = casePreconditions;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
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

    public Integer getDefectProcessingCount() {
        return defectProcessingCount;
    }

    public void setDefectProcessingCount(Integer defectProcessingCount) {
        this.defectProcessingCount = defectProcessingCount;
    }

    public Integer getDefectAuditCount() {
        return defectAuditCount;
    }

    public void setDefectAuditCount(Integer defectAuditCount) {
        this.defectAuditCount = defectAuditCount;
    }

    public Integer getDefectCloseCount() {
        return defectCloseCount;
    }

    public void setDefectCloseCount(Integer defectCloseCount) {
        this.defectCloseCount = defectCloseCount;
    }

    public String getCaseStepScript() {
        return caseStepScript;
    }

    public void setCaseStepScript(String caseStepScript) {
        this.caseStepScript = caseStepScript;
    }

    public String getImgObjects() {
        return imgObjects;
    }

    public void setImgObjects(String imgObjects) {
        this.imgObjects = imgObjects;
    }

    public Date getCaseExportUpdateTime() {
        return caseExportUpdateTime;
    }

    public void setCaseExportUpdateTime(Date caseExportUpdateTime) {
        this.caseExportUpdateTime = caseExportUpdateTime;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public SysPlanItem getPlanItem() {
        return planItem;
    }

    public void setPlanItem(SysPlanItem planItem) {
        this.planItem = planItem;
    }

    public List<SysUser> getFocusList() {
        return focusList;
    }

    public void setFocusList(List<SysUser> focusList) {
        this.focusList = focusList;
    }
}
