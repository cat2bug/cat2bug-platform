package com.cat2bug.common.domain.im;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IMBasePlatformConfig {

    @JsonProperty("switch")
    private boolean configSwitch;

    public IMBasePlatformConfig() {
    }

    public IMBasePlatformConfig(boolean configSwitch) {
        this.configSwitch = configSwitch;
    }

    public boolean isConfigSwitch() {
        return configSwitch;
    }

    public void setConfigSwitch(boolean configSwitch) {
        this.configSwitch = configSwitch;
    }
}
