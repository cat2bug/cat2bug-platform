package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.cat2bug.system.domain.SysUserTeamRole;
import com.cat2bug.system.service.ISysUserTeamRoleService;

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

    /**
     * 查询用户团队角色
     * 
     * @param userTeamId 用户团队角色主键
     * @return 用户团队角色
     */
    @Override
    public SysUserTeamRole selectSysUserTeamRoleByUserTeamId(Long userTeamId)
    {
        return sysUserTeamRoleMapper.selectSysUserTeamRoleByUserTeamId(userTeamId);
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
        sysUserTeamRole.setCreateTime(DateUtils.getNowDate());
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
        sysUserTeamRole.setUpdateTime(DateUtils.getNowDate());
        return sysUserTeamRoleMapper.updateSysUserTeamRole(sysUserTeamRole);
    }

    @Override
    public int updateSysUserTeamRoleByTeamIdAndMemberId(Long teamId, Long memberId, SysUserTeamRole sysUserTeamRole) {
        sysUserTeamRole.setTeamId(teamId);
        sysUserTeamRole.setUserId(memberId);
        return sysUserTeamRoleMapper.updateSysUserTeamRoleByTeamIdAndMemberId(sysUserTeamRole);
    }

    /**
     * 批量删除用户团队角色
     * 
     * @param userTeamIds 需要删除的用户团队角色主键
     * @return 结果
     */
    @Override
    public int deleteSysUserTeamRoleByUserTeamIds(Long[] userTeamIds)
    {
        return sysUserTeamRoleMapper.deleteSysUserTeamRoleByUserTeamIds(userTeamIds);
    }

    /**
     * 删除用户团队角色信息
     * 
     * @param userTeamId 用户团队角色主键
     * @return 结果
     */
    @Override
    public int deleteSysUserTeamRoleByUserTeamId(Long userTeamId)
    {
        return sysUserTeamRoleMapper.deleteSysUserTeamRoleByUserTeamId(userTeamId);
    }
}
