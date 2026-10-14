package com.cat2bug.quarkus.common.domain.im;

public class IMDingPlatformConfig extends IMBasePlatformConfig {

    private Boolean singleSwitch;
    private Boolean groupSwitch;
    private String key;
    private String secret;
    private String hook;
    private String mobile;

    public IMDingPlatformConfig() {
    }

    public IMDingPlatformConfig(boolean configSwitch, Boolean singleSwitch, Boolean groupSwitch,
                                String key, String secret, String hook, String mobile) {
        super(configSwitch);
        this.singleSwitch = singleSwitch;
        this.groupSwitch = groupSwitch;
        this.key = key;
        this.secret = secret;
        this.hook = hook;
        this.mobile = mobile;
    }

    public Boolean getSingleSwitch() {
        return singleSwitch;
    }

    public void setSingleSwitch(Boolean singleSwitch) {
        this.singleSwitch = singleSwitch;
    }

    public Boolean getGroupSwitch() {
        return groupSwitch;
    }

    public void setGroupSwitch(Boolean groupSwitch) {
        this.groupSwitch = groupSwitch;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getHook() {
        return hook;
    }

    public void setHook(String hook) {
        this.hook = hook;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
