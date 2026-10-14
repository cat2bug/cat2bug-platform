package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysProjectDefectTabs;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.domain.SysUserProjectRole;
import com.cat2bug.common.domain.vo.BatchUserRoleVo;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class SysUserProjectService {

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Inject
    SysUserProjectRoleMapper sysUserProjectRoleMapper;

    @Inject
    SysProjectDefectTabsService sysProjectDefectTabsService;

    public TableDataInfo selectMemberPage(Long projectId, String search, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysUserProjectMapper.countSysUserListByProjectId(projectId, search);
        List<SysUser> rows = MemberViewHelper.withRoleIds(
                sysUserProjectMapper.selectSysUserListByProjectId(projectId, search, page.getOffset(), page.getPageSize()));
        return new TableDataInfo(rows, total);
    }

    public TableDataInfo selectNotMemberPage(Long projectId, String search, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysUserProjectMapper.countNotSysUserListByProjectId(projectId, search);
        List<SysUser> rows = sysUserProjectMapper.selectNotSysUserListByProjectId(
                projectId, search, page.getOffset(), page.getPageSize());
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

    @Transactional
    public int addMember(Long projectId, Long userId) {
        SysUserProject up = new SysUserProject();
        up.setProjectId(projectId);
        up.setUserId(userId);
        up.setProjectLock(0);
        return sysUserProjectMapper.insertSysUserProject(up);
    }

    public int updateCollect(SysUserProject sysUserProject) {
        return sysUserProjectMapper.updateSysUserProjectByUserIdAndProjectId(sysUserProject);
    }

    public int deleteMember(Long projectId, Long memberId, Long currentUserId) {
        if (memberId.equals(currentUserId)) {
            throw new ApiException("不能删除当前登录用户");
        }
        return sysUserProjectMapper.deleteSysUserProjectByProjectIdAndUserId(projectId, memberId);
    }

    public List<SysUser> selectSysUserListByProjectId(Long projectId) {
        return sysUserProjectMapper.selectSysUserListByProjectId(projectId, null, 0, Integer.MAX_VALUE);
    }
}
