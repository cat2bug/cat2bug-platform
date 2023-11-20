package com.cat2bug.system.service.impl;

import java.util.List;
import java.util.Optional;

import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysUserTeamRole;
import com.cat2bug.system.mapper.SysUserTeamRoleMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserTeamMapper;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.service.ISysUserTeamService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户团队角色Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-20
 */
@Service
public class SysUserTeamServiceImpl implements ISysUserTeamService 
{
    @Autowired
    private SysUserTeamMapper sysUserTeamMapper;

    /**
     * 查询用户团队角色
     * 
     * @param userTeamId 用户团队角色主键
     * @return 用户团队角色
     */
    @Override
    public SysUserTeam selectSysUserTeamByUserTeamId(Long userTeamId)
    {
        return sysUserTeamMapper.selectSysUserTeamByUserTeamId(userTeamId);
    }

    /**
     * 查询用户团队角色列表
     * 
     * @param sysUserTeam 用户团队角色
     * @return 用户团队角色
     */
    @Override
    public List<SysUserTeam> selectSysUserTeamList(SysUserTeam sysUserTeam)
    {
        return sysUserTeamMapper.selectSysUserTeamList(sysUserTeam);
    }

    /**
     * 新增用户团队角色
     * 
     * @param sysUserTeam 用户团队角色
     * @return 结果
     */
    @Override
    public int insertSysUserTeam(SysUserTeam sysUserTeam)
    {
        sysUserTeam.setCreateTime(DateUtils.getNowDate());
        return sysUserTeamMapper.insertSysUserTeam(sysUserTeam);
    }

    /**
     * 修改用户团队角色
     * 
     * @param sysUserTeam 用户团队角色
     * @return 结果
     */
    @Override
    public int updateSysUserTeam(SysUserTeam sysUserTeam)
    {
        sysUserTeam.setUpdateTime(DateUtils.getNowDate());
        return sysUserTeamMapper.updateSysUserTeam(sysUserTeam);
    }

    @Override
    public int updateSysUserTeamByTeamIdAndMemberId(Long teamId, Long memberId, SysUserTeam sysUserTeam) {
        sysUserTeam.setTeamId(teamId);
        sysUserTeam.setUserId(memberId);
        return sysUserTeamMapper.updateSysUserTeamByTeamIdAndMemberId(sysUserTeam);
    }

    /**
     * 批量删除用户团队角色
     * 
     * @param userTeamIds 需要删除的用户团队角色主键
     * @return 结果
     */
    @Override
    public int deleteSysUserTeamByUserTeamIds(Long[] userTeamIds)
    {
        return sysUserTeamMapper.deleteSysUserTeamByUserTeamIds(userTeamIds);
    }

    /**
     * 删除用户团队角色信息
     * 
     * @param userTeamId 用户团队角色主键
     * @return 结果
     */
    @Override
    public int deleteSysUserTeamByUserTeamId(Long userTeamId)
    {
        return sysUserTeamMapper.deleteSysUserTeamByUserTeamId(userTeamId);
    }
}
