package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysUserTeamRole;

/**
 * 用户团队角色Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-20
 */
public interface ISysUserTeamRoleService 
{
    /**
     * 查询用户团队角色
     * 
     * @param userTeamRoleId 用户团队角色主键
     * @return 用户团队角色
     */
    public SysUserTeamRole selectSysUserTeamRoleByUserTeamRoleId(Long userTeamRoleId);

    /**
     * 查询用户团队角色列表
     * 
     * @param sysUserTeamRole 用户团队角色
     * @return 用户团队角色集合
     */
    public List<SysUserTeamRole> selectSysUserTeamRoleList(SysUserTeamRole sysUserTeamRole);

    /**
     * 新增用户团队角色
     * 
     * @param sysUserTeamRole 用户团队角色
     * @return 结果
     */
    public int insertSysUserTeamRole(SysUserTeamRole sysUserTeamRole);

    /**
     * 修改用户团队角色
     * 
     * @param sysUserTeamRole 用户团队角色
     * @return 结果
     */
    public int updateSysUserTeamRole(SysUserTeamRole sysUserTeamRole);

    /**
     * 修改用户团队角色
     * @param teamId    团队id
     * @param memberId  用户id
     * @param roleIds   角色id集合
     * @return  结果
     */
    public int updateSysUserTeamRoleByTeamIdAndMemberIdAndRoleIds(Long teamId, Long memberId, Long[] roleIds);
    /**
     * 批量删除用户团队角色
     * 
     * @param userTeamRoleIds 需要删除的用户团队角色主键集合
     * @return 结果
     */
    public int deleteSysUserTeamRoleByUserTeamRoleIds(Long[] userTeamRoleIds);

    /**
     * 删除用户团队角色信息
     * 
     * @param userTeamRoleId 用户团队角色主键
     * @return 结果
     */
    public int deleteSysUserTeamRoleByUserTeamRoleId(Long userTeamRoleId);
}
