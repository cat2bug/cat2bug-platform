package com.cat2bug.quarkus.common.domain;

public class SysProjectPush {

    private String version;
    private String pushKey;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPushKey() {
        return pushKey;
    }

    public void setPushKey(String pushKey) {
        this.pushKey = pushKey;
    }
}
