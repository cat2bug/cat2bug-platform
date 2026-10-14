package com.cat2bug.quarkus.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SysDefect {

    private Long defectId;
    private Long projectNum;
    private Integer defectType;
    private String defectName;
    /** 缺陷列表合并关键字（名称/编号/版本/交付物/处理人昵称） */
    private String nameVersionKeyword;
    private String defectDescribe;
    private String defectLevel;
    private Integer defectState;
    private Long moduleId;
    private String moduleName;
    private String moduleVersion;
    private Long projectId;
    private Long caseId;
    private Long testPlanId;
    private Long caseStepId;
    private String createBy;
    private Long createById;
    private SysUser createMember;
    private String createMemberName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String updateBy;
    private Long updateById;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private List<Long> handleBy;
    private List<SysUser> handleByList = new ArrayList<>();
    private List<SysUser> focusList;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;
    private String imgUrls;
    private String annexUrls;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planEndTime;
    private String delFlag;
    private Map<String, Object> params;
    private boolean collect;
    private int rejectCount;

    /** Excel 导入/导出：类型显示名 */
    private String defectTypeImportName;
    /** Excel 导入/导出：状态显示名 */
    private String defectStateImportName;
    /** Excel 导入/导出：处理人昵称 */
    private String handleByNames;
    /** 交付物全路径（导出/导入用，非持久化） */
    private String modulePath;
    /** 自定义字段 JSON */
    private Map<String, Object> customFields = new LinkedHashMap<>();

    public Long getDefectId() {
        return defectId;
    }

    public void setDefectId(Long defectId) {
        this.defectId = defectId;
    }

    public Long getProjectNum() {
        return projectNum;
    }

    public void setProjectNum(Long projectNum) {
        this.projectNum = projectNum;
    }

    public Integer getDefectType() {
        return defectType;
    }

    public void setDefectType(Integer defectType) {
        this.defectType = defectType;
    }

    public String getDefectName() {
        return defectName;
    }

    public void setDefectName(String defectName) {
        this.defectName = defectName;
    }

    public String getNameVersionKeyword() {
        return nameVersionKeyword;
    }

    public void setNameVersionKeyword(String nameVersionKeyword) {
        this.nameVersionKeyword = nameVersionKeyword;
    }

    public String getDefectDescribe() {
        return defectDescribe;
    }

    public void setDefectDescribe(String defectDescribe) {
        this.defectDescribe = defectDescribe;
    }

    public String getDefectLevel() {
        return defectLevel;
    }

    public void setDefectLevel(String defectLevel) {
        this.defectLevel = defectLevel;
    }

    public Integer getDefectState() {
        return defectState;
    }

    public void setDefectState(Integer defectState) {
        this.defectState = defectState;
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

    public String getModuleVersion() {
        return moduleVersion;
    }

    public void setModuleVersion(String moduleVersion) {
        this.moduleVersion = moduleVersion;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getTestPlanId() {
        return testPlanId;
    }

    public void setTestPlanId(Long testPlanId) {
        this.testPlanId = testPlanId;
    }

    public Long getCaseStepId() {
        return caseStepId;
    }

    public void setCaseStepId(Long caseStepId) {
        this.caseStepId = caseStepId;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Long getCreateById() {
        return createById;
    }

    public void setCreateById(Long createById) {
        this.createById = createById;
    }

    public SysUser getCreateMember() {
        return createMember;
    }

    public void setCreateMember(SysUser createMember) {
        this.createMember = createMember;
    }

    public String getCreateMemberName() {
        return createMemberName;
    }

    public void setCreateMemberName(String createMemberName) {
        this.createMemberName = createMemberName;
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

    public List<Long> getHandleBy() {
        return handleBy;
    }

    public void setHandleBy(List<Long> handleBy) {
        this.handleBy = handleBy;
    }

    public List<SysUser> getHandleByList() {
        return handleByList;
    }

    public void setHandleByList(List<SysUser> handleByList) {
        this.handleByList = handleByList;
    }

    public List<SysUser> getFocusList() {
        return focusList;
    }

    public void setFocusList(List<SysUser> focusList) {
        this.focusList = focusList;
    }

    public Date getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Date handleTime) {
        this.handleTime = handleTime;
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

    public Date getPlanStartTime() {
        return planStartTime;
    }

    public void setPlanStartTime(Date planStartTime) {
        this.planStartTime = planStartTime;
    }

    public Date getPlanEndTime() {
        return planEndTime;
    }

    public void setPlanEndTime(Date planEndTime) {
        this.planEndTime = planEndTime;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean isCollect() {
        return collect;
    }

    public void setCollect(boolean collect) {
        this.collect = collect;
    }

    public int getRejectCount() {
        return rejectCount;
    }

    public void setRejectCount(int rejectCount) {
        this.rejectCount = rejectCount;
    }

    public String getDefectTypeImportName() {
        return defectTypeImportName;
    }

    public void setDefectTypeImportName(String defectTypeImportName) {
        this.defectTypeImportName = defectTypeImportName;
    }

    public String getDefectStateImportName() {
        return defectStateImportName;
    }

    public void setDefectStateImportName(String defectStateImportName) {
        this.defectStateImportName = defectStateImportName;
    }

    public String getHandleByNames() {
        return handleByNames;
    }

    public void setHandleByNames(String handleByNames) {
        this.handleByNames = handleByNames;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
