package com.cat2bug.common.domain.im;

public class EnterpriseWeChatPlatformConfig extends IMBasePlatformConfig {

    private Boolean singleSwitch;
    private Boolean groupSwitch;
    private String mobile;
    private String hook;

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

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getHook() {
        return hook;
    }

    public void setHook(String hook) {
        this.hook = hook;
    }
}
