package com.cat2bug.quarkus.common.domain.im;

public class IMProjectConfig {

    private Long configId;
    private Long projectId;
    private String systemCode;
    private String configParams;

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getSystemCode() {
        return systemCode;
    }

    public void setSystemCode(String systemCode) {
        this.systemCode = systemCode;
    }

    public String getConfigParams() {
        return configParams;
    }

    public void setConfigParams(String configParams) {
        this.configParams = configParams;
    }
}
