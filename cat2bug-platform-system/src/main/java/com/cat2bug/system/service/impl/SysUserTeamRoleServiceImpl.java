package com.cat2bug.system.service.impl;

import java.util.List;
import java.util.Optional;

import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.cat2bug.system.domain.SysUserTeamRole;
import com.cat2bug.system.service.ISysUserTeamRoleService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户团队角色Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-20
 */
@Service
public class SysUserTeamRoleServiceImpl implements ISysUserTeamRoleService 
{
    @Autowired
    private SysUserTeamRoleMapper sysUserTeamRoleMapper;

    @Autowired
    private SysUserTeamMapper sysUserTeamMapper;

    /**
     * 查询用户团队角色
     * 
     * @param userTeamRoleId 用户团队角色主键
     * @return 用户团队角色
     */
    @Override
    public SysUserTeamRole selectSysUserTeamRoleByUserTeamRoleId(Long userTeamRoleId)
    {
        return sysUserTeamRoleMapper.selectSysUserTeamRoleByUserTeamRoleId(userTeamRoleId);
    }

    /**
     * 查询用户团队角色列表
     * 
     * @param sysUserTeamRole 用户团队角色
     * @return 用户团队角色
     */
    @Override
    public List<SysUserTeamRole> selectSysUserTeamRoleList(SysUserTeamRole sysUserTeamRole)
    {
        return sysUserTeamRoleMapper.selectSysUserTeamRoleList(sysUserTeamRole);
    }

    /**
     * 新增用户团队角色
     * 
     * @param sysUserTeamRole 用户团队角色
     * @return 结果
     */
    @Override
    public int insertSysUserTeamRole(SysUserTeamRole sysUserTeamRole)
    {
        return sysUserTeamRoleMapper.insertSysUserTeamRole(sysUserTeamRole);
    }

    /**
     * 修改用户团队角色
     * 
     * @param sysUserTeamRole 用户团队角色
     * @return 结果
     */
    @Override
    public int updateSysUserTeamRole(SysUserTeamRole sysUserTeamRole)
    {
        return sysUserTeamRoleMapper.updateSysUserTeamRole(sysUserTeamRole);
    }

    @Transactional
    @Override
    public int updateSysUserTeamRoleByTeamIdAndMemberIdAndRoleIds(Long teamId, Long memberId, Long[] roleIds) {
        SysUserTeam ut = new SysUserTeam();
        ut.setTeamId(teamId);
        ut.setUserId(memberId);
        Optional<SysUserTeam> sysUserTeam = sysUserTeamMapper.selectSysUserTeamList(ut).stream().findFirst();
        Preconditions.checkState(sysUserTeam.isPresent(), MessageUtils.message("team.not_found_member"));

        sysUserTeamRoleMapper.deleteSysUserTeamByUserTeamId(sysUserTeam.get().getUserTeamId());
        if(roleIds!=null) {
            for(Long roleId : roleIds) {
                SysUserTeamRole utr = new SysUserTeamRole();
                utr.setUserTeamId(sysUserTeam.get().getUserTeamId());
                utr.setRoleId(roleId);
                Preconditions.checkState(sysUserTeamRoleMapper.insertSysUserTeamRole(utr)==1,MessageUtils.message("team.update_member_role_fail"));
            }
        }

        return 1;
    }

    /**
     * 批量删除用户团队角色
     * 
     * @param userTeamRoleIds 需要删除的用户团队角色主键
     * @return 结果
     */
    @Override
    public int deleteSysUserTeamRoleByUserTeamRoleIds(Long[] userTeamRoleIds)
    {
        return sysUserTeamRoleMapper.deleteSysUserTeamRoleByUserTeamRoleIds(userTeamRoleIds);
    }

    /**
     * 删除用户团队角色信息
     * 
     * @param userTeamRoleId 用户团队角色主键
     * @return 结果
     */
    @Override
    public int deleteSysUserTeamRoleByUserTeamRoleId(Long userTeamRoleId)
    {
        return sysUserTeamRoleMapper.deleteSysUserTeamRoleByUserTeamRoleId(userTeamRoleId);
    }
}
