package com.cat2bug.common.domain;

public class SysCaseType {

    private Long typeId;
    private Long projectId;
    private String typeName;
    private Integer sortOrder;
    /** 1 启用 / 0 停用 */
    private Integer enabled;
    private Integer caseCount;

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getCaseCount() {
        return caseCount;
    }

    public void setCaseCount(Integer caseCount) {
        this.caseCount = caseCount;
    }
}
