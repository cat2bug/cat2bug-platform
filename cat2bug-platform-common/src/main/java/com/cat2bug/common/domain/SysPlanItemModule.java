package com.cat2bug.common.domain;

public class SysPlanItemModule {

    private Long moduleId;
    private Long modulePid;
    private String moduleName;
    private String remark;
    private Long projectId;
    private String modulePath;
    private Long childrenCount;
    private String planId;
    private long itemCount;
    private long passCount;
    private long defectCount;

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public Long getModulePid() {
        return modulePid;
    }

    public void setModulePid(Long modulePid) {
        this.modulePid = modulePid;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public Long getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(Long childrenCount) {
        this.childrenCount = childrenCount;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

    public long getPassCount() {
        return passCount;
    }

    public void setPassCount(long passCount) {
        this.passCount = passCount;
    }

    public long getDefectCount() {
        return defectCount;
    }

    public void setDefectCount(long defectCount) {
        this.defectCount = defectCount;
    }
}
