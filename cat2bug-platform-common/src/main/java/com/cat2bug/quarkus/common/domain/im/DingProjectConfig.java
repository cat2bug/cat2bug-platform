package com.cat2bug.quarkus.common.domain.im;

public class DingProjectConfig {

    public static final String SYSTEM_CODE = "ding";

    private String appKey;
    private String appSecret;
    private String robotCode;

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getRobotCode() {
        return robotCode;
    }

    public void setRobotCode(String robotCode) {
        this.robotCode = robotCode;
    }
}
