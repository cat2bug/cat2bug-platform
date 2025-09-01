package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysUserTeam;

/**
 * 用户团队角色Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-20
 */
public interface ISysUserTeamService 
{
    /**
     * 查询用户团队角色
     * 
     * @param userTeamId 用户团队角色主键
     * @return 用户团队角色
     */
    public SysUserTeam selectSysUserTeamByUserTeamId(Long userTeamId);

    /**
     * 查询用户团队角色列表
     * 
     * @param sysUserTeam 用户团队角色
     * @return 用户团队角色集合
     */
    public List<SysUserTeam> selectSysUserTeamList(SysUserTeam sysUserTeam);

    /**
     * 新增用户团队角色
     * 
     * @param sysUserTeam 用户团队角色
     * @return 结果
     */
    public int insertSysUserTeam(SysUserTeam sysUserTeam);

    /**
     * 修改用户团队角色
     * 
     * @param sysUserTeam 用户团队角色
     * @return 结果
     */
    public int updateSysUserTeam(SysUserTeam sysUserTeam);

    /**
     * 修改用户团队角色
     * @param teamId            团队id
     * @param memberId          用户id
     * @param sysUserTeam   用户团队角色
     * @return  结果
     */
    public int updateSysUserTeamByTeamIdAndMemberId(Long teamId, Long memberId, SysUserTeam sysUserTeam);

    /**
     * 批量删除用户团队角色
     * 
     * @param userTeamIds 需要删除的用户团队角色主键集合
     * @return 结果
     */
    public int deleteSysUserTeamByUserTeamIds(Long[] userTeamIds);

    /**
     * 删除用户团队角色信息
     * 
     * @param userTeamId 用户团队角色主键
     * @return 结果
     */
    public int deleteSysUserTeamByUserTeamId(Long userTeamId);
}
