package com.cat2bug.quarkus.common.domain.im;

public class EnterpriseWeChatProjectConfig {

    public static final String SYSTEM_CODE = "wechat";

    private String corpId;
    private String agentid;
    private String corpSecret;

    public String getCorpId() {
        return corpId;
    }

    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }

    public String getAgentid() {
        return agentid;
    }

    public void setAgentid(String agentid) {
        this.agentid = agentid;
    }

    public String getCorpSecret() {
        return corpSecret;
    }

    public void setCorpSecret(String corpSecret) {
        this.corpSecret = corpSecret;
    }
}
