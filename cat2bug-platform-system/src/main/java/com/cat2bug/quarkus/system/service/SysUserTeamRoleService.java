package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.SysUserTeam;
import com.cat2bug.quarkus.common.domain.SysUserTeamRole;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.system.mapper.SysUserTeamMapper;
import com.cat2bug.quarkus.system.mapper.SysUserTeamRoleMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SysUserTeamRoleService {

    @Inject
    SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Inject
    SysUserTeamMapper sysUserTeamMapper;

    @Transactional
    public int updateSysUserTeamRoleByTeamIdAndMemberIdAndRoleIds(Long teamId, Long memberId, Long[] roleIds) {
        SysUserTeam query = new SysUserTeam();
        query.setTeamId(teamId);
        query.setUserId(memberId);
        List<SysUserTeam> members = sysUserTeamMapper.selectSysUserTeamList(query);
        if (members == null || members.isEmpty()) {
            throw new ApiException("团队中没有找到此成员!");
        }
        SysUserTeam sysUserTeam = members.get(0);

        sysUserTeamRoleMapper.deleteSysUserTeamByUserTeamId(sysUserTeam.getUserTeamId());
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysUserTeamRole utr = new SysUserTeamRole();
                utr.setUserTeamId(sysUserTeam.getUserTeamId());
                utr.setRoleId(roleId);
                if (sysUserTeamRoleMapper.insertSysUserTeamRole(utr) != 1) {
                    throw new ApiException("更新团队成员角色失败!");
                }
            }
        }
        return 1;
    }
}
