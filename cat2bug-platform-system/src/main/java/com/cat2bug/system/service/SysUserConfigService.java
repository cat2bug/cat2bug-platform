package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysUserConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SysUserConfigService {

    @Inject
    SysUserConfigMapper sysUserConfigMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    public SysUserConfig selectSysUserConfigByUserConfigId(Long userConfigId) {
        return sysUserConfigMapper.selectSysUserConfigByUserConfigId(userConfigId);
    }

    public SysUserConfig selectSysUserConfigByUserId(Long userId) {
        return sysUserConfigMapper.selectSysUserConfigByUserId(userId);
    }

    public int updateSysUserConfig(Long userId, SysUserConfig sysUserConfig) {
        // 只改 currentProjectId 时同步所属团队，避免侧栏 teamId 与项目不一致、菜单命名空间重写失败
        syncTeamIdFromProject(userId, sysUserConfig);
        SysUserConfig existing = sysUserConfigMapper.selectSysUserConfigByUserId(userId);
        if (existing == null) {
            sysUserConfig.setUserId(userId);
            return sysUserConfigMapper.insertSysUserConfig(sysUserConfig);
        }
        sysUserConfig.setUserConfigId(existing.getUserConfigId());
        sysUserConfig.setUserId(existing.getUserId());
        return sysUserConfigMapper.updateSysUserConfig(sysUserConfig);
    }

    private void syncTeamIdFromProject(Long userId, SysUserConfig sysUserConfig) {
        if (sysUserConfig == null) {
            return;
        }
        Long projectId = sysUserConfig.getCurrentProjectId();
        if (projectId == null || projectId <= 0 || sysUserConfig.getCurrentTeamId() != null) {
            return;
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, userId);
        if (project != null && project.getTeamId() != null) {
            sysUserConfig.setCurrentTeamId(project.getTeamId());
        }
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
