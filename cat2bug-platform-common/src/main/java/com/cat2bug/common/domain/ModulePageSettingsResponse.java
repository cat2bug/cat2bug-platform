package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * 模块页面设置读接口：设置本体摊平，并附带系统上限与当前有效上传上限（只读，不落库）。
 */
public class ModulePageSettingsResponse {

    @JsonUnwrapped
    private ModulePageSettings settings;
    private int systemMaxUploadMb;
    private int effectiveMaxUploadMb;

    public ModulePageSettings getSettings() {
        return settings;
    }

    public void setSettings(ModulePageSettings settings) {
        this.settings = settings;
    }

    public int getSystemMaxUploadMb() {
        return systemMaxUploadMb;
    }

    public void setSystemMaxUploadMb(int systemMaxUploadMb) {
        this.systemMaxUploadMb = systemMaxUploadMb;
    }

    public int getEffectiveMaxUploadMb() {
        return effectiveMaxUploadMb;
    }

    public void setEffectiveMaxUploadMb(int effectiveMaxUploadMb) {
        this.effectiveMaxUploadMb = effectiveMaxUploadMb;
    }
}
