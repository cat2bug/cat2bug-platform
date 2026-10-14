package com.cat2bug.quarkus.common.domain.im;

public class IMMailPlatformConfig extends IMBasePlatformConfig {

    private String receiver;

    public IMMailPlatformConfig() {
    }

    public IMMailPlatformConfig(boolean configSwitch, String receiver) {
        super(configSwitch);
        this.receiver = receiver;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
}
