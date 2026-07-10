package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.domain.SysUserProject;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysUserProjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysUserProjectService {

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

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
