package com.cat2bug.system.service;

import java.util.List;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysTeam;

/**
 * 团队Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
public interface ISysTeamService 
{
    /**
     * 查询团队
     * 
     * @param teamId 团队主键
     * @return 团队
     */
    public SysTeam selectSysTeamByTeamId(Long teamId);

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
     * 查询成员列表
     * @param teamId    团队id
     * @return          成员集合
     */
    public List<SysUser> selectSysUserListByTeamId(Long teamId);

    /**
     * 新增团队
     * 
     * @param sysTeam 团队
     * @return 结果
     */
    public int insertSysTeam(SysTeam sysTeam);

    /**
     * 修改团队
     * 
     * @param sysTeam 团队
     * @return 结果
     */
    public int updateSysTeam(SysTeam sysTeam);

    /**
     * 批量删除团队
     * 
     * @param teamIds 需要删除的团队主键集合
     * @return 结果
     */
    public int deleteSysTeamByTeamIds(Long[] teamIds);

    /**
     * 删除团队信息
     * 
     * @param teamId 团队主键
     * @return 结果
     */
    public int deleteSysTeamByTeamId(Long teamId);
}
