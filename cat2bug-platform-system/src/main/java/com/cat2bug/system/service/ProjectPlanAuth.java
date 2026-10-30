package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 甘特/排期特权：项目创建人、项目管理员可改任意计划节点、冻基线、写工作日历。
 */
@ApplicationScoped
public class ProjectPlanAuth {

    private static final Set<String> MANAGE_ROLE_KEYS = Set.of("project.create-by", "project.admin");

    @Inject
    SysRoleMapper sysRoleMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    public boolean isProjectPlanAdmin(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return false;
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        if (project != null && project.getCreateBy() != null && !project.getCreateBy().isBlank()) {
            try {
                if (userId.equals(Long.parseLong(project.getCreateBy().trim()))) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // createBy 也可能是用户名，继续看角色
            }
        }
        List<SysRole> roles = sysRoleMapper.selectRolePermissionByUserId(null, projectId, userId);
        if (roles == null) {
            return false;
        }
        for (SysRole role : roles) {
            if (role != null && MANAGE_ROLE_KEYS.contains(role.getRoleKey())) {
                return true;
            }
        }
        return false;
    }

    public void requireProjectPlanAdmin(Long projectId, Long userId) {
        if (!isProjectPlanAdmin(projectId, userId)) {
            throw new ApiException(403, "仅项目创建人或项目管理员可执行此操作");
        }
    }

    /**
     * 非管理员须在缺陷处理人列表中。
     */
    public boolean canEditPlanNode(Long projectId, Long userId, List<Long> handleBy) {
        if (isProjectPlanAdmin(projectId, userId)) {
            return true;
        }
        if (userId == null || handleBy == null || handleBy.isEmpty()) {
            return false;
        }
        for (Long id : handleBy) {
            if (Objects.equals(userId, id)) {
                return true;
            }
        }
        return false;
    }

    public void requireCanEditPlanNode(Long projectId, Long userId, List<Long> handleBy) {
        if (!canEditPlanNode(projectId, userId, handleBy)) {
            throw new ApiException(403, "只能修改处理人为自己的缺陷计划");
        }
    }
}
