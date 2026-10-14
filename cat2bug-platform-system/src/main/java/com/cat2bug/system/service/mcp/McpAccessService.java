package com.cat2bug.system.service.mcp;

import com.cat2bug.common.constant.SecurityConstants;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysProjectMcp;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.security.McpModules;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysProjectMcpMapper;
import com.cat2bug.system.mapper.SysRoleMapper;
import com.cat2bug.system.service.SysPermissionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按（用户，项目）计算 MCP 有效权限，不读写 {@code sys_user_config}，因此不会改变用户在网页上的当前项目。
 * 结果短时缓存；改项目 MCP 设置时立即清除，成员角色变更最多延迟 {@link #CACHE_TTL_MILLIS} 生效。
 */
@ApplicationScoped
public class McpAccessService {

    static final long CACHE_TTL_MILLIS = 30_000L;

    private record Cached(McpProjectAccess access, long at) {
    }

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Inject
    SysProjectMapper projectMapper;

    @Inject
    SysProjectMcpMapper projectMcpMapper;

    @Inject
    SysRoleMapper roleMapper;

    @Inject
    SysPermissionService permissionService;

    public McpProjectAccess access(SysUser user, Long projectId) {
        String key = user.getUserId() + ":" + projectId;
        Cached cached = cache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.at() < CACHE_TTL_MILLIS) {
            return cached.access();
        }
        McpProjectAccess access = compute(user, projectId);
        cache.put(key, new Cached(access, now));
        return access;
    }

    public void invalidateProject(Long projectId) {
        String suffix = ":" + projectId;
        cache.keySet().removeIf(k -> k.endsWith(suffix));
    }

    public void invalidateAll() {
        cache.clear();
    }

    public List<SysProject> enabledProjectsOf(Long userId) {
        return projectMcpMapper.selectEnabledProjectsOfUser(userId);
    }

    private McpProjectAccess compute(SysUser user, Long projectId) {
        Long userId = user.getUserId();
        SysProject project = projectId == null ? null : projectMapper.selectSysProjectByProjectId(projectId, userId);
        if (project == null || project.getProjectState() == null || project.getProjectState() <= 0) {
            return new McpProjectAccess(userId, project, false, false, false, List.of(), List.of(), Set.of());
        }
        SysProjectMcp settings = projectMcpMapper.selectByProjectId(projectId);
        boolean enabled = settings != null && settings.isEnabled();
        List<String> modules = settings == null ? List.of() : McpModules.normalize(settings.getModules());
        boolean admin = SysUser.isAdmin(userId);
        boolean member = projectMcpMapper.countMember(projectId, userId) > 0;

        List<SysRole> roles = new ArrayList<>();
        Set<String> perms = new HashSet<>();
        if (admin) {
            perms.add(SecurityConstants.ALL_PERMISSION);
        } else if (member) {
            for (SysRole role : roleMapper.selectRolePermissionByUserId(project.getTeamId(), projectId, userId)) {
                if (role != null && "0".equals(role.getStatus())) {
                    role.setFlag(true);
                    roles.add(role);
                }
            }
            SysUser scoped = new SysUser();
            scoped.setUserId(userId);
            scoped.setRoles(roles);
            perms.addAll(permissionService.getMenuPermission(scoped));
        }
        return new McpProjectAccess(userId, project, admin, member, enabled, List.copyOf(modules),
                List.copyOf(roles), Set.copyOf(perms));
    }
}
