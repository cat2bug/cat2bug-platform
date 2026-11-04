package com.cat2bug.system.service;

import com.cat2bug.common.util.ModulePageSettingsSupport;
import com.cat2bug.common.util.UploadLimits;
import com.cat2bug.framework.storage.UploadByteLimit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** 系统上传上限，以及能解析到项目时的团队有效上限。 */
@ApplicationScoped
public class SysUploadLimitService implements UploadByteLimit {

    @Inject
    SysModulePageSettingsService modulePageSettingsService;

    @Override
    public long systemMaxBytes() {
        return UploadLimits.toBytes(modulePageSettingsService.systemMaxMb());
    }

    @Override
    public long effectiveMaxBytes(Long projectId) {
        int system = modulePageSettingsService.systemMaxMb();
        if (projectId == null) {
            return UploadLimits.toBytes(system);
        }
        try {
            return UploadLimits.toBytes(ModulePageSettingsSupport.effectiveMaxMb(
                    modulePageSettingsService.getByProjectId(projectId), system));
        } catch (RuntimeException e) {
            return UploadLimits.toBytes(system);
        }
    }
}
