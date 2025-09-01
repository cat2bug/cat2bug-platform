package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysUserTeam;
import org.apache.ibatis.annotations.Param;

/**
 * 用户团队角色Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-20
 */
public interface SysUserTeamMapper 
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
     *
     * @param sysUserTeam 用户团队角色
     * @return 结果
     */
    public int updateSysUserTeamByTeamIdAndMemberId(SysUserTeam sysUserTeam);

    /**
     * 删除用户团队角色
     * @param teamId    团队id
     * @param memberId  成员id
     * @param roleIds   排除的角色id集合
     * @return  结果
     */
    public int deleteSysUserTeamByTeamIdAndMemberIdAndRoleIds(@Param("teamId") Long teamId, @Param("memberId") Long memberId, @Param("roleIds") Long[] roleIds);

    /**
     * 删除用户团队角色
     * 
     * @param userTeamId 用户团队角色主键
     * @return 结果
     */
    public int deleteSysUserTeamByUserTeamId(Long userTeamId);

    /**
     * 批量删除用户团队角色
     * 
     * @param userTeamIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserTeamByUserTeamIds(Long[] userTeamIds);
}
