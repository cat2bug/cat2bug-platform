package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.util.PathKeyValidator;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.service.mcp.SysProjectMcpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SysProjectService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 与 Spring 一致：项目创建人角色 project.create-by */
    private static final Long PROJECT_CREATE_BY_ROLE_ID = 11L;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysProjectMcpService sysProjectMcpService;

    @Inject
    SysCaseTypeService sysCaseTypeService;

    @Inject
    SysTeamPlanService sysTeamPlanService;

    @Inject
    ProjectMembershipAuth projectMembershipAuth;

    public TableDataInfo selectProjectPage(Long userId, SysProject query, Integer pageNum, Integer pageSize) {
        if (query.getTeamId() == null) {
            throw new ApiException("团队不能为空");
        }
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysProjectMapper.countSysProjectList(userId, query);
        List<SysProject> rows = sysProjectMapper.selectSysProjectList(userId, query, page.getOffset(), page.getPageSize());
        // 与管理端列表一致：补齐成员，供前端「成员」列与进入项目成员校验（members 为空会误报无权限）
        rows.forEach(project -> project.setMembers(
                sysUserProjectMapper.selectSysUserListByProjectId(project.getProjectId(), null, null, 0, Integer.MAX_VALUE)));
        return new TableDataInfo(rows, total);
    }

    public List<SysProject> selectProjectListAll(Long userId, SysProject query) {
        if (query.getTeamId() == null) {
            throw new ApiException("团队不能为空");
        }
        return sysProjectMapper.selectSysProjectList(userId, query, 0, Integer.MAX_VALUE);
    }

    public TableDataInfo selectAdminProjectPage(SysProject query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysProjectMapper.countAdminSysProjectList(query);
        List<SysProject> rows = sysProjectMapper.selectAdminSysProjectList(query, page.getOffset(), page.getPageSize());
        rows.forEach(project -> project.setMembers(
                sysUserProjectMapper.selectSysUserListByProjectId(project.getProjectId(), null, null, 0, Integer.MAX_VALUE)));
        return new TableDataInfo(rows, total);
    }

    public SysProject selectSysProjectByProjectId(Long projectId, Long userId) {
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, userId);
        if (project == null) {
            return null;
        }
        // userId 非空表示「进入项目」语义：非成员不得假成功（LEFT JOIN 仍会查出项目行）
        if (userId != null && !projectMembershipAuth.canAccessProject(userId, projectId)) {
            return null;
        }
        return project;
    }

    /**
     * 按 teamKey + projectKey 解析项目（前端 path 守卫 / 分享链接）。
     */
    public SysProject selectSysProjectByTeamAndProjectKey(String teamKey, String projectKey, Long userId) {
        if (teamKey == null || teamKey.isBlank() || projectKey == null || projectKey.isBlank()) {
            throw new ApiException(404, MessageUtils.message("git.repo.project-not-found"));
        }
        SysProject resolved = sysProjectMapper.selectSysProjectByTeamAndProjectKey(teamKey.trim(), projectKey.trim());
        if (resolved == null || resolved.getProjectId() == null) {
            throw new ApiException(404, MessageUtils.message("git.repo.project-not-found"));
        }
        SysProject accessible = selectSysProjectByProjectId(resolved.getProjectId(), userId);
        if (accessible == null) {
            throw new ApiException(403, MessageUtils.message("no-permission"));
        }
        return accessible;
    }

    @Transactional
    public int insertSysProject(SysProject sysProject, Long userId) {
        if (sysProject.getTeamId() == null) {
            throw new ApiException("团队不能为空");
        }
        if (sysProject.getProjectName() == null || sysProject.getProjectName().isBlank()) {
            throw new ApiException("项目名称不能为空");
        }
        String projectKey = PathKeyValidator.normalizeOrNull(sysProject.getProjectKey());
        if (projectKey == null) {
            throw new ApiException(400, MessageUtils.message("project.path-key-invalid"));
        }
        if (sysProjectMapper.countByTeamIdAndProjectKey(sysProject.getTeamId(), projectKey, null) != 0) {
            throw new ApiException(400, MessageUtils.message("project.path-key-duplicate"));
        }
        sysTeamPlanService.assertCanCreateProject(sysProject.getTeamId());
        sysProject.setProjectKey(projectKey);
        String now = LocalDateTime.now().format(TIME_FMT);
        sysProject.setProjectState(1);
        sysProject.setCreateBy(String.valueOf(userId));
        sysProject.setCreateTime(now);
        sysProject.setUpdateBy(String.valueOf(userId));
        sysProject.setUpdateTime(now);
        if (sysProjectMapper.insertSysProject(sysProject) != 1) {
            throw new ApiException("创建项目失败");
        }
        insertProjectMembers(sysProject, userId);
        sysProjectMcpService.createDefault(sysProject.getProjectId(), String.valueOf(userId));
        sysCaseTypeService.ensureTeamDefaultsIfEmpty(sysProject.getProjectId());
        return 1;
    }

    /**
     * 写入创建表单中的项目成员（按 userId 去重）；若未包含创建人则自动补上，并确保创建人带 project.create-by。
     */
    private void insertProjectMembers(SysProject sysProject, Long userId) {
        Map<Long, SysUser> byId = new LinkedHashMap<>();
        List<SysUser> members = sysProject.getMembers();
        if (members != null) {
            for (SysUser member : members) {
                if (member == null || member.getUserId() == null) {
                    continue;
                }
                byId.putIfAbsent(member.getUserId(), member);
            }
        }
        if (!byId.containsKey(userId)) {
            SysUser creator = new SysUser();
            creator.setUserId(userId);
            creator.setRoleIds(new Long[]{PROJECT_CREATE_BY_ROLE_ID});
            byId.put(userId, creator);
        }
        for (SysUser member : byId.values()) {
            Long[] roleIds = resolveCreateMemberRoleIds(member, userId);
            BatchUserRoleVo batch = new BatchUserRoleVo();
            batch.setProjectId(sysProject.getProjectId());
            batch.setMemberIds(new Long[]{member.getUserId()});
            batch.setRoleIds(roleIds);
            sysUserProjectService.batchInsertSysUserProject(batch);
        }
    }

    private Long[] resolveCreateMemberRoleIds(SysUser member, Long creatorUserId) {
        LinkedHashSet<Long> roles = new LinkedHashSet<>();
        if (member.getRoleIds() != null) {
            for (Long roleId : member.getRoleIds()) {
                if (roleId != null) {
                    roles.add(roleId);
                }
            }
        }
        if (creatorUserId.equals(member.getUserId())) {
            roles.add(PROJECT_CREATE_BY_ROLE_ID);
        }
        if (roles.isEmpty()) {
            throw new ApiException(MessageUtils.message("project.member_role_not_empty"));
        }
        return roles.toArray(Long[]::new);
    }
    public int updateSysProject(SysProject sysProject, Long userId) {
        applyProjectKeyOnUpdate(sysProject);
        sysProject.setUpdateBy(String.valueOf(userId));
        sysProject.setUpdateTime(LocalDateTime.now().format(TIME_FMT));
        return sysProjectMapper.updateSysProject(sysProject);
    }

    public int updateSysProject(SysProject sysProject) {
        applyProjectKeyOnUpdate(sysProject);
        sysProject.setUpdateTime(LocalDateTime.now().format(TIME_FMT));
        return sysProjectMapper.updateSysProject(sysProject);
    }

    /**
     * 更新时若带了 projectKey：规范化并校验团队内唯一，避免唯一索引冲突落到「服务器内部错误」。
     * 未传 / 空串时视为不改路径键（如管理端仅改锁定状态）。
     */
    private void applyProjectKeyOnUpdate(SysProject sysProject) {
        if (sysProject == null || sysProject.getProjectId() == null) {
            throw new ApiException("项目不能为空");
        }
        if (sysProject.getProjectKey() == null || sysProject.getProjectKey().isBlank()) {
            return;
        }
        String projectKey = PathKeyValidator.normalizeOrNull(sysProject.getProjectKey());
        if (projectKey == null) {
            throw new ApiException(400, MessageUtils.message("project.path-key-invalid"));
        }
        Long teamId = sysProject.getTeamId();
        if (teamId == null) {
            SysProject existing = sysProjectMapper.selectSysProjectByProjectId(sysProject.getProjectId(), null);
            if (existing == null || existing.getTeamId() == null) {
                throw new ApiException(404, MessageUtils.message("git.repo.project-not-found"));
            }
            teamId = existing.getTeamId();
        }
        if (sysProjectMapper.countByTeamIdAndProjectKey(teamId, projectKey, sysProject.getProjectId()) != 0) {
            throw new ApiException(400, MessageUtils.message("project.path-key-duplicate"));
        }
        sysProject.setProjectKey(projectKey);
    }

    public int deleteSysProjectByProjectId(Long projectId) {
        return sysProjectMapper.deleteSysProjectByProjectId(projectId);
    }
}
