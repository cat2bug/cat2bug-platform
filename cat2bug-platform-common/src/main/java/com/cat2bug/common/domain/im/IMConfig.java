package com.cat2bug.common.domain.im;

import java.util.Map;

public class IMConfig {

    private Map<String, Object> modules;
    private IMPlatformConfig platforms;

    public Map<String, Object> getModules() {
        return modules;
    }

    public void setModules(Map<String, Object> modules) {
        this.modules = modules;
    }

    public IMPlatformConfig getPlatforms() {
        return platforms;
    }

    public void setPlatforms(IMPlatformConfig platforms) {
        this.platforms = platforms;
    }
}
