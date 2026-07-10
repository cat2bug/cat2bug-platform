package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysUserProject;
import com.cat2bug.quarkus.common.domain.SysUserProjectRole;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysUserProjectMapper;
import com.cat2bug.quarkus.system.mapper.SysUserProjectRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysUserProjectRoleService {

    @Inject
    SysUserProjectRoleMapper sysUserProjectRoleMapper;

    @Inject
    SysUserProjectMapper sysUserProjectMapper;

    @Transactional
    public int updateSysUserProjectRole(Long projectId, Long memberId, List<Long> roleIds) {
        SysUserProject query = new SysUserProject();
        query.setUserId(memberId);
        query.setProjectId(projectId);
        List<SysUserProject> members = sysUserProjectMapper.selectSysUserProjectList(query);
        if (members == null || members.isEmpty()) {
            throw new ApiException("项目中没有找到此成员!");
        }
        Long userProjectId = members.get(0).getUserProjectId();

        sysUserProjectRoleMapper.deleteSysUserProjectRoleByUserProjectId(userProjectId);
        int ret = 0;
        for (Long roleId : roleIds) {
            SysUserProjectRole upr = new SysUserProjectRole();
            upr.setRoleId(roleId);
            upr.setUserProjectId(userProjectId);
            ret += sysUserProjectRoleMapper.insertSysUserProjectRole(upr);
        }
        return ret;
    }
}
