package com.cat2bug.common.domain;

public class SysUserStatisticTemplate {

    private Long statisticTemplateId;
    private String statisticTemplatCode;
    private Long moduleType;
    private Long projectId;
    private Long userId;
    private String statisticTemplatConfig;

    public Long getStatisticTemplateId() {
        return statisticTemplateId;
    }

    public void setStatisticTemplateId(Long statisticTemplateId) {
        this.statisticTemplateId = statisticTemplateId;
    }

    public String getStatisticTemplatCode() {
        return statisticTemplatCode;
    }

    public void setStatisticTemplatCode(String statisticTemplatCode) {
        this.statisticTemplatCode = statisticTemplatCode;
    }

    public Long getModuleType() {
        return moduleType;
    }

    public void setModuleType(Long moduleType) {
        this.moduleType = moduleType;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatisticTemplatConfig() {
        return statisticTemplatConfig;
    }

    public void setStatisticTemplatConfig(String statisticTemplatConfig) {
        this.statisticTemplatConfig = statisticTemplatConfig;
    }
}
