package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.domain.SysUserProjectRole;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.util.PathKeyValidator;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import com.cat2bug.system.service.mcp.SysProjectMcpService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    SysUserProjectRoleMapper sysUserProjectRoleMapper;

    @Inject
    SysProjectMcpService sysProjectMcpService;

    @Inject
    SysCaseTypeService sysCaseTypeService;
    public TableDataInfo selectProjectPage(Long userId, SysProject query, Integer pageNum, Integer pageSize) {
        if (query.getTeamId() == null) {
            throw new ApiException("团队不能为空");
        }
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysProjectMapper.countSysProjectList(userId, query);
        List<SysProject> rows = sysProjectMapper.selectSysProjectList(userId, query, page.getOffset(), page.getPageSize());
        // 与管理端列表一致：补齐成员，供前端「成员」列与进入项目成员校验（members 为空会误报无权限）
        rows.forEach(project -> project.setMembers(
                sysUserProjectMapper.selectSysUserListByProjectId(project.getProjectId(), null, 0, Integer.MAX_VALUE)));
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
                sysUserProjectMapper.selectSysUserListByProjectId(project.getProjectId(), null, 0, Integer.MAX_VALUE)));
        return new TableDataInfo(rows, total);
    }

    public SysProject selectSysProjectByProjectId(Long projectId, Long userId) {
        return sysProjectMapper.selectSysProjectByProjectId(projectId, userId);
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
        SysProject accessible = sysProjectMapper.selectSysProjectByProjectId(resolved.getProjectId(), userId);
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
        SysUserProject up = new SysUserProject();
        up.setProjectId(sysProject.getProjectId());
        up.setUserId(userId);
        up.setProjectLock(0);
        if (sysUserProjectMapper.insertSysUserProject(up) != 1 || up.getUserProjectId() == null) {
            throw new ApiException("创建项目成员失败");
        }
        SysUserProjectRole role = new SysUserProjectRole();
        role.setUserProjectId(up.getUserProjectId());
        role.setRoleId(PROJECT_CREATE_BY_ROLE_ID);
        if (sysUserProjectRoleMapper.insertSysUserProjectRole(role) != 1) {
            throw new ApiException("创建项目角色失败");
        }
        sysProjectMcpService.createDefault(sysProject.getProjectId(), String.valueOf(userId));
        sysCaseTypeService.seedDefaults(sysProject.getProjectId());
        return 1;
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
