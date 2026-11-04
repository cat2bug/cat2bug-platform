package com.cat2bug.system.service;

import com.cat2bug.common.domain.DefectPageSettings;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.DefectPageSettingsSupport;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysTeamMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SysDefectPageSettingsService {

    @Inject
    SysTeamMapper sysTeamMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    public DefectPageSettings getByTeamId(Long teamId) {
        requireTeam(teamId);
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException("团队不存在");
        }
        return DefectPageSettingsSupport.parseOrDefault(team.getDefectPageConfig());
    }

    public DefectPageSettings getByProjectId(Long projectId) {
        Long teamId = resolveTeamIdByProject(projectId);
        return getByTeamId(teamId);
    }

    @Transactional
    public DefectPageSettings saveByTeamId(Long teamId, DefectPageSettings body) {
        requireTeam(teamId);
        SysTeam existing = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (existing == null) {
            throw new ApiException("团队不存在");
        }
        DefectPageSettings normalized = DefectPageSettingsSupport.normalizeAndValidate(body);
        SysTeam update = new SysTeam();
        update.setTeamId(teamId);
        update.setDefectPageConfig(DefectPageSettingsSupport.toJson(normalized));
        if (sysTeamMapper.updateSysTeam(update) <= 0) {
            throw new ApiException("保存页面设置失败");
        }
        return normalized;
    }

    public boolean isShareAllowedForProject(Long projectId) {
        if (projectId == null) {
            return true;
        }
        try {
            return getByProjectId(projectId).isAllowShare();
        } catch (ApiException e) {
            return true;
        }
    }

    public boolean isShareAllowedForTeam(Long teamId) {
        if (teamId == null) {
            return true;
        }
        return getByTeamId(teamId).isAllowShare();
    }

    public void assertShareAllowedForDefectProject(Long projectId) {
        if (!isShareAllowedForProject(projectId)) {
            throw new ApiException(403, DefectPageSettingsSupport.shareDisabledMessage());
        }
    }

    private Long resolveTeamIdByProject(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        if (project == null || project.getTeamId() == null) {
            throw new ApiException("项目不存在");
        }
        return project.getTeamId();
    }

    private static void requireTeam(Long teamId) {
        if (teamId == null) {
            throw new ApiException("团队不能为空");
        }
    }
}
