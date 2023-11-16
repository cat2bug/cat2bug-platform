package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.SysUserTeamRole;

/**
 * 团队Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
public interface SysTeamMapper 
{
    /**
     * 查询团队
     * 
     * @param teamId 团队主键
     * @return 团队
     */
    public SysTeam selectSysTeamByTeamId(Long teamId);

    /**
     * 查询团队
     * @param teamName  团队名称
     * @return  团队
     */
    public SysTeam selectSysTeamByTeamName(String teamName);

    /**
     * 查询团队列表
     * 
     * @param sysTeam 团队
     * @return 团队集合
     */
    public List<SysTeam> selectSysTeamList(SysTeam sysTeam);

    /**
     * 查询团队列表
     * @param userId    用户id
     * @return  团队集合
     */
    public List<SysTeam> selectSysTeamListByUserId(Long userId);

    /**
     * 新增团队
     * 
     * @param sysTeam 团队
     * @return 结果
     */
    public int insertSysTeam(SysTeam sysTeam);

    /**
     * 新增用户团队角色
     * @param sysUserTeamRole
     * @return
     */
    public int insertSysUserTeamRole(SysUserTeamRole sysUserTeamRole);

    /**
     * 修改团队
     * 
     * @param sysTeam 团队
     * @return 结果
     */
    public int updateSysTeam(SysTeam sysTeam);

    /**
     * 修改用户团队角色
     * @param sysUserTeamRole
     * @return
     */
    public int updateSysUserTeamRole(SysUserTeamRole sysUserTeamRole);
    /**
     * 删除团队
     * 
     * @param teamId 团队主键
     * @return 结果
     */
    public int deleteSysTeamByTeamId(Long teamId);

    /**
     * 批量删除团队
     * 
     * @param teamIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysTeamByTeamIds(Long[] teamIds);
}
