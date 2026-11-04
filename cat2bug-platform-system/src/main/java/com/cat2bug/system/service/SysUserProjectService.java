package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysGitRepositoryMember;
import com.cat2bug.common.domain.SysProjectDefectTabs;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.domain.SysUserProjectRole;
import com.cat2bug.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.common.domain.vo.ProjectMemberRemovePreview;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SysUserProjectService {

    /** 项目创建人 project.create-by */
    private static final Long PROJECT_CREATE_BY_ROLE_ID = 11L;
    /** 项目管理员 project.admin */
    private static final Long PROJECT_ADMIN_ROLE_ID = 6L;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    SysUserProjectRoleMapper sysUserProjectRoleMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysProjectDefectTabsService sysProjectDefectTabsService;

    @Inject
    ProjectMembershipAuth projectMembershipAuth;

    @Inject
    SysDefectService sysDefectService;

    @Inject
    ISysGitRepositoryMemberService sysGitRepositoryMemberService;

    public TableDataInfo selectMemberPage(Long projectId, String search, Long roleId, Integer pageNum, Integer pageSize) {
        Long filterRoleId = (roleId == null || roleId == 0L) ? null : roleId;
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysUserProjectMapper.countSysUserListByProjectId(projectId, search, filterRoleId);
        List<SysUser> rows = MemberViewHelper.withRoleIds(
                sysUserProjectMapper.selectSysUserListByProjectId(
                        projectId, search, filterRoleId, page.getOffset(), page.getPageSize()));
        return new TableDataInfo(rows, total);
    }

    public TableDataInfo selectNotMemberPage(Long projectId, String search, Integer pageNum, Integer pageSize) {
        if (search == null || search.isBlank()) {
            return new TableDataInfo(List.of(), 0);
        }
        PageDomain page = PageDomain.of(pageNum, pageSize);
        String keyword = search.trim();
        long total = sysUserProjectMapper.countNotSysUserListByProjectId(projectId, keyword);
        List<SysUser> rows = sysUserProjectMapper.selectNotSysUserListByProjectId(
                projectId, keyword, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    /** 本项目所属团队中尚未加入项目的成员；允许空 search。 */
    public TableDataInfo selectTeamNotMemberPage(Long projectId, String search, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        String keyword = (search == null || search.isBlank()) ? null : search.trim();
        long total = sysUserProjectMapper.countTeamNotMemberListByProjectId(projectId, keyword);
        List<SysUser> rows = sysUserProjectMapper.selectTeamNotMemberListByProjectId(
                projectId, keyword, page.getOffset(), page.getPageSize());
        return new TableDataInfo(rows, total);
    }

    @Transactional
    public int batchInsertSysUserProject(BatchUserRoleVo batchUserRoleVo) {
        if (batchUserRoleVo.getMemberIds() == null || batchUserRoleVo.getMemberIds().length == 0) {
            throw new ApiException(MessageUtils.message("project.add_member_fail"));
        }
        if (batchUserRoleVo.getRoleIds() == null || batchUserRoleVo.getRoleIds().length == 0) {
            throw new ApiException(MessageUtils.message("project.member_role_not_empty"));
        }
        int ret = 0;
        for (Long memberId : batchUserRoleVo.getMemberIds()) {
            SysUserProject up = new SysUserProject();
            up.setProjectId(batchUserRoleVo.getProjectId());
            up.setUserId(memberId);
            up.setProjectLock(0);
            ret += sysUserProjectMapper.insertSysUserProject(up);
            if (up.getUserProjectId() == null) {
                throw new ApiException(MessageUtils.message("project.add_member_fail"));
            }

            SysProjectDefectTabs tab = new SysProjectDefectTabs();
            tab.setUserId(up.getUserId());
            tab.setProjectId(up.getProjectId());
            tab.setTabName(MessageUtils.message("my"));
            SysDefect sysDefect = new SysDefect();
            sysDefect.setHandleBy(Arrays.asList(up.getUserId()));
            Map<String, Object> params = new HashMap<>();
            params.put("defectStates", new ArrayList<>(SysProjectDefectTabsService.MY_TAB_DEFECT_STATES));
            params.put("delFlag", "0");
            sysDefect.setParams(params);
            tab.setConfig(sysDefect);
            if (sysProjectDefectTabsService.insertSysProjectDefectTabs(tab) == null) {
                throw new ApiException(MessageUtils.message("project.add_member_fail"));
            }

            for (Long roleId : batchUserRoleVo.getRoleIds()) {
                SysUserProjectRole upr = new SysUserProjectRole();
                upr.setUserProjectId(up.getUserProjectId());
                upr.setRoleId(roleId);
                if (sysUserProjectRoleMapper.insertSysUserProjectRole(upr) <= 0) {
                    throw new ApiException(MessageUtils.message("project.add_member_fail"));
                }
            }
        }
        return ret;
    }

    public int updateCollect(SysUserProject sysUserProject) {
        return sysUserProjectMapper.updateSysUserProjectByUserIdAndProjectId(sysUserProject);
    }

    public boolean isProjectCreator(Long projectId, Long userId) {
        if (projectId == null || userId == null) {
            return false;
        }
        return sysUserProjectRoleMapper.countByProjectUserAndRole(projectId, userId, PROJECT_CREATE_BY_ROLE_ID) > 0;
    }

    /**
     * 转让项目创建人：仅当前创建人可调用；目标须为本项目成员；同步 sys_project.create_by。
     */
    @Transactional
    public int transferCreator(Long projectId, Long operatorId, Long targetUserId) {
        if (projectId == null || operatorId == null || targetUserId == null) {
            throw new ApiException(400, "参数无效");
        }
        if (operatorId.equals(targetUserId)) {
            throw new ApiException(400, "不能转让给自己");
        }
        if (!isProjectCreator(projectId, operatorId)) {
            throw new ApiException(403, "仅项目创建人可转让");
        }
        SysUserProject target = findUserProject(projectId, targetUserId);
        if (target == null || target.getUserProjectId() == null) {
            throw new ApiException(400, "受让人须为本项目成员");
        }
        SysUserProject operator = findUserProject(projectId, operatorId);
        if (operator == null || operator.getUserProjectId() == null) {
            throw new ApiException(403, "仅项目创建人可转让");
        }

        sysUserProjectRoleMapper.deleteSysUserProjectRoleByUserProjectId(target.getUserProjectId());
        SysUserProjectRole creatorLink = new SysUserProjectRole();
        creatorLink.setUserProjectId(target.getUserProjectId());
        creatorLink.setRoleId(PROJECT_CREATE_BY_ROLE_ID);
        if (sysUserProjectRoleMapper.insertSysUserProjectRole(creatorLink) != 1) {
            throw new ApiException("转让创建人失败");
        }

        sysUserProjectRoleMapper.deleteByUserProjectIdAndRoleId(operator.getUserProjectId(), PROJECT_CREATE_BY_ROLE_ID);
        List<Long> remain = sysUserProjectRoleMapper.selectRoleIdsByUserProjectId(operator.getUserProjectId());
        if (remain == null || remain.isEmpty()) {
            SysUserProjectRole admin = new SysUserProjectRole();
            admin.setUserProjectId(operator.getUserProjectId());
            admin.setRoleId(PROJECT_ADMIN_ROLE_ID);
            if (sysUserProjectRoleMapper.insertSysUserProjectRole(admin) != 1) {
                throw new ApiException("转让后写入原创建人角色失败");
            }
        }

        if (sysProjectMapper.updateCreateBy(projectId, String.valueOf(targetUserId)) != 1) {
            throw new ApiException("更新项目创建人字段失败");
        }
        return 1;
    }

    public ProjectMemberRemovePreview previewRemoveMember(Long projectId, Long memberId) {
        if (projectId == null || memberId == null) {
            throw new ApiException(400, "项目或成员无效");
        }
        if (!projectMembershipAuth.isProjectMember(memberId, projectId)) {
            throw new ApiException(404, "成员不在该项目中");
        }
        if (isProjectCreator(projectId, memberId)) {
            throw new ApiException(400, "项目创建人不可删除，请先转让创建人角色");
        }
        ProjectMemberRemovePreview preview = new ProjectMemberRemovePreview();
        SysDefectService.MemberHandlerImpact impact = sysDefectService.findOpenHandlerImpact(projectId, memberId);
        for (SysDefect defect : impact.soleHandlers) {
            ProjectMemberRemovePreview.DefectSummary summary = new ProjectMemberRemovePreview.DefectSummary();
            summary.setDefectId(defect.getDefectId());
            summary.setProjectNum(defect.getProjectNum());
            summary.setDefectName(defect.getDefectName());
            summary.setDefectState(defect.getDefectState());
            preview.getSoleHandlerDefects().add(summary);
        }
        preview.setMultiHandlerDefectCount(impact.multiHandlers.size());

        for (SysGitRepositoryMember row : sysGitRepositoryMemberService.listAclByProjectMember(projectId, memberId)) {
            ProjectMemberRemovePreview.RepoSummary repo = new ProjectMemberRemovePreview.RepoSummary();
            repo.setRepositoryId(row.getRepositoryId());
            repo.setName(row.getRepositoryName());
            repo.setSlug(row.getRepositorySlug());
            if (sysGitRepositoryMemberService.isLastAdmin(row.getRepositoryId(), memberId)) {
                preview.getLastAdminRepositories().add(repo);
            } else {
                preview.getOtherAclRepositories().add(repo);
            }
        }

        preview.setRequiresHandoff(
                !preview.getSoleHandlerDefects().isEmpty() || !preview.getLastAdminRepositories().isEmpty());
        return preview;
    }

    @Transactional
    public int deleteMember(Long projectId, Long memberId, Long currentUserId, Long handoffUserId) {
        if (memberId.equals(currentUserId)) {
            throw new ApiException("不能删除当前登录用户");
        }
        if (!projectMembershipAuth.isProjectMember(memberId, projectId)) {
            throw new ApiException(404, "成员不在该项目中");
        }
        if (isProjectCreator(projectId, memberId)) {
            throw new ApiException(400, "项目创建人不可删除，请先转让创建人角色");
        }
        ProjectMemberRemovePreview preview = previewRemoveMember(projectId, memberId);
        if (preview.isRequiresHandoff()) {
            if (handoffUserId == null) {
                throw new ApiException(400, "移除成员前须指定交接人");
            }
            if (handoffUserId.equals(memberId)) {
                throw new ApiException(400, "交接人不能是被移除的成员");
            }
            if (!projectMembershipAuth.isProjectMember(handoffUserId, projectId)) {
                throw new ApiException(400, "交接人必须是项目成员");
            }
        }

        sysDefectService.applyHandleByOnProjectMemberRemoval(projectId, memberId, handoffUserId);
        sysGitRepositoryMemberService.syncRemoveOnProjectMemberRemoval(
                projectId, memberId, preview.isRequiresHandoff() ? handoffUserId : null);

        // 不在此处清零被移出者的 currentProjectId：保留后其后续请求可命中
        // 「您已被移出该项目」文案，由前端弹框引导回项目列表后再清上下文。
        return sysUserProjectMapper.deleteSysUserProjectByProjectIdAndUserId(projectId, memberId);
    }

    /** 兼容旧调用：无交接人。 */
    public int deleteMember(Long projectId, Long memberId, Long currentUserId) {
        return deleteMember(projectId, memberId, currentUserId, null);
    }

    public List<SysUser> selectSysUserListByProjectId(Long projectId) {
        return sysUserProjectMapper.selectSysUserListByProjectId(projectId, null, null, 0, Integer.MAX_VALUE);
    }

    private SysUserProject findUserProject(Long projectId, Long userId) {
        SysUserProject query = new SysUserProject();
        query.setProjectId(projectId);
        query.setUserId(userId);
        List<SysUserProject> rows = sysUserProjectMapper.selectSysUserProjectList(query);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }
}
