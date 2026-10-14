package com.cat2bug.quarkus.common.domain.im;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class IMPlatformConfig {

    @JsonProperty("asystem")
    private IMSystemPlatformConfig system;

    @JsonProperty("bmail")
    private IMMailPlatformConfig mail;

    private IMDingPlatformConfig ding;
    private EnterpriseWeChatPlatformConfig wechat;
    private FeishuPlatformConfig feishu;

    public IMSystemPlatformConfig getSystem() {
        return system;
    }

    public void setSystem(IMSystemPlatformConfig system) {
        this.system = system;
    }

    public IMMailPlatformConfig getMail() {
        return mail;
    }

    public void setMail(IMMailPlatformConfig mail) {
        this.mail = mail;
    }

    public IMDingPlatformConfig getDing() {
        return ding;
    }

    public void setDing(IMDingPlatformConfig ding) {
        this.ding = ding;
    }

    public EnterpriseWeChatPlatformConfig getWechat() {
        return wechat;
    }

    public void setWechat(EnterpriseWeChatPlatformConfig wechat) {
        this.wechat = wechat;
    }

    public FeishuPlatformConfig getFeishu() {
        return feishu;
    }

    public void setFeishu(FeishuPlatformConfig feishu) {
        this.feishu = feishu;
    }
}
