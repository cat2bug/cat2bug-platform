package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysUserConfig;
import com.cat2bug.quarkus.system.mapper.SysUserConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SysUserConfigService {

    @Inject
    SysUserConfigMapper sysUserConfigMapper;

    public SysUserConfig selectSysUserConfigByUserConfigId(Long userConfigId) {
        return sysUserConfigMapper.selectSysUserConfigByUserConfigId(userConfigId);
    }

    public SysUserConfig selectSysUserConfigByUserId(Long userId) {
        return sysUserConfigMapper.selectSysUserConfigByUserId(userId);
    }

    public int updateSysUserConfig(Long userId, SysUserConfig sysUserConfig) {
        SysUserConfig existing = sysUserConfigMapper.selectSysUserConfigByUserId(userId);
        if (existing == null) {
            sysUserConfig.setUserId(userId);
            return sysUserConfigMapper.insertSysUserConfig(sysUserConfig);
        }
        sysUserConfig.setUserConfigId(existing.getUserConfigId());
        sysUserConfig.setUserId(existing.getUserId());
        return sysUserConfigMapper.updateSysUserConfig(sysUserConfig);
    }

    public int insertSysUserConfig(SysUserConfig sysUserConfig) {
        return sysUserConfigMapper.insertSysUserConfig(sysUserConfig);
    }

    public int deleteSysUserConfigByUserConfigIds(Long[] userConfigIds, Long ownerUserId) {
        if (userConfigIds == null || userConfigIds.length == 0) {
            return 0;
        }
        return sysUserConfigMapper.deleteSysUserConfigByUserConfigIds(userConfigIds, ownerUserId);
    }
}
