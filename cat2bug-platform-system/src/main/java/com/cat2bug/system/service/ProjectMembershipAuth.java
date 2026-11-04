package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysUserConfigMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 项目进入 / 当前项目上下文：须为 {@code sys_user_project} 成员（系统超管豁免）。
 */
@ApplicationScoped
public class ProjectMembershipAuth {

    /** 前端据此弹「已移出项目」引导；勿随意改文案。 */
    public static final String PROJECT_REMOVED_MESSAGE = "您已被移出该项目";

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    SysUserConfigMapper sysUserConfigMapper;

    public boolean isSystemAdmin(Long userId) {
        return SysUser.isAdmin(userId);
    }

    public boolean isProjectMember(Long userId, Long projectId) {
        if (userId == null || projectId == null || projectId <= 0) {
            return false;
        }
        SysUserProject query = new SysUserProject();
        query.setUserId(userId);
        query.setProjectId(projectId);
        List<SysUserProject> rows = sysUserProjectMapper.selectSysUserProjectList(query);
        return rows != null && !rows.isEmpty();
    }

    /**
     * 系统超管或项目成员可访问项目上下文与项目业务读。
     */
    public boolean canAccessProject(Long userId, Long projectId) {
        if (userId == null || projectId == null || projectId <= 0) {
            return false;
        }
        if (isSystemAdmin(userId)) {
            return true;
        }
        return isProjectMember(userId, projectId);
    }

    public void requireProjectAccess(Long userId, Long projectId) {
        if (canAccessProject(userId, projectId)) {
            return;
        }
        // 仅当拒绝的正是「当前项目」时返回可识别文案，供前端弹框跳转；切到其它无权项目仍用通用提示
        if (isCurrentProject(userId, projectId)) {
            throw new ApiException(403, PROJECT_REMOVED_MESSAGE);
        }
        throw new ApiException(403, "无权限访问该项目");
    }

    public boolean isCurrentProject(Long userId, Long projectId) {
        if (userId == null || projectId == null || projectId <= 0) {
            return false;
        }
        SysUserConfig config = sysUserConfigMapper.selectSysUserConfigByUserId(userId);
        return config != null && projectId.equals(config.getCurrentProjectId());
    }

    /**
     * 若 currentProjectId 非成员则持久化为 0，并返回清洗后的配置（原地修改或新建副本）。
     */
    public SysUserConfig sanitizeCurrentProject(Long userId, SysUserConfig config) {
        if (config == null || userId == null) {
            return config;
        }
        Long projectId = config.getCurrentProjectId();
        if (projectId == null || projectId <= 0) {
            return config;
        }
        if (canAccessProject(userId, projectId)) {
            return config;
        }
        config.setCurrentProjectId(0L);
        SysUserConfig patch = new SysUserConfig();
        patch.setUserConfigId(config.getUserConfigId());
        patch.setUserId(userId);
        patch.setCurrentProjectId(0L);
        patch.setCurrentTeamId(config.getCurrentTeamId());
        if (config.getUserConfigId() != null) {
            sysUserConfigMapper.updateSysUserConfig(patch);
        }
        return config;
    }

    /**
     * 移出项目成员后：若其当前项目正是该项目则清零。
     */
    public void clearCurrentProjectIfMatches(Long userId, Long projectId) {
        if (userId == null || projectId == null || projectId <= 0) {
            return;
        }
        SysUserConfig config = sysUserConfigMapper.selectSysUserConfigByUserId(userId);
        if (config == null || config.getCurrentProjectId() == null) {
            return;
        }
        if (!projectId.equals(config.getCurrentProjectId())) {
            return;
        }
        SysUserConfig patch = new SysUserConfig();
        patch.setUserConfigId(config.getUserConfigId());
        patch.setUserId(userId);
        patch.setCurrentProjectId(0L);
        patch.setCurrentTeamId(config.getCurrentTeamId());
        sysUserConfigMapper.updateSysUserConfig(patch);
    }
}
