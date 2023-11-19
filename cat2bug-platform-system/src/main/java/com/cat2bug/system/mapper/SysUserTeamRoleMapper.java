package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysUserTeamRole;

/**
 * 用户团队角色Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-20
 */
public interface SysUserTeamRoleMapper 
{
    /**
     * 查询用户团队角色
     * 
     * @param userTeamId 用户团队角色主键
     * @return 用户团队角色
     */
    public SysUserTeamRole selectSysUserTeamRoleByUserTeamId(Long userTeamId);

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
     *
     * @param sysUserTeamRole 用户团队角色
     * @return 结果
     */
    public int updateSysUserTeamRoleByTeamIdAndMemberId(SysUserTeamRole sysUserTeamRole);

    /**
     * 删除用户团队角色
     * 
     * @param userTeamId 用户团队角色主键
     * @return 结果
     */
    public int deleteSysUserTeamRoleByUserTeamId(Long userTeamId);

    /**
     * 批量删除用户团队角色
     * 
     * @param userTeamIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserTeamRoleByUserTeamIds(Long[] userTeamIds);
}
