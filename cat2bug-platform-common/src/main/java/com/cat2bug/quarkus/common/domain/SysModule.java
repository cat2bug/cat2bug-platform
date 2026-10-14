package com.cat2bug.quarkus.common.domain;

import java.util.ArrayList;
import java.util.List;

public class SysModule {

    private Long moduleId;
    private Long modulePid;
    private String moduleName;
    private String remark;
    private Long projectId;
    private int childrenCount;
    private String modulePath;
    private String annexUrls;
    private List<String> batchModuleNames;
    private List<SysModule> children = new ArrayList<>();

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

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public String getAnnexUrls() {
        return annexUrls;
    }

    public void setAnnexUrls(String annexUrls) {
        this.annexUrls = annexUrls;
    }

    public List<String> getBatchModuleNames() {
        return batchModuleNames;
    }

    public void setBatchModuleNames(List<String> batchModuleNames) {
        this.batchModuleNames = batchModuleNames;
    }

    public List<SysModule> getChildren() {
        return children;
    }

    public void setChildren(List<SysModule> children) {
        this.children = children;
    }
}
