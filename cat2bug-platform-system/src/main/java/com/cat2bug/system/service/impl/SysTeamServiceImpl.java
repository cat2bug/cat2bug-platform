package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.SysUserTeamRole;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysTeamMapper;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.service.ISysTeamService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 团队Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
@Service
public class SysTeamServiceImpl implements ISysTeamService 
{
    /** 默认规则ID */
    private final static Long DEFAULT_ROLE_ID = 0L;

    @Autowired
    private SysTeamMapper sysTeamMapper;

    /**
     * 查询团队
     * 
     * @param teamId 团队主键
     * @return 团队
     */
    @Override
    public SysTeam selectSysTeamByTeamId(Long teamId)
    {
        return sysTeamMapper.selectSysTeamByTeamId(teamId);
    }

    /**
     * 查询团队列表
     * 
     * @param sysTeam 团队
     * @return 团队
     */
    @Override
    public List<SysTeam> selectSysTeamList(SysTeam sysTeam)
    {
        return sysTeamMapper.selectSysTeamList(sysTeam);
    }

    /**
     * 新增团队
     * 
     * @param sysTeam 团队
     * @return 结果
     */
    @Override
    @Transactional
    public int insertSysTeam(SysTeam sysTeam)
    {
        Preconditions.checkNotNull(sysTeam.getTeamName(),MessageUtils.message("team.insert_team_name_cannot_empty"));

        // 检测团队名称是否已经使用
        SysTeam selectSysTeam = sysTeamMapper.selectSysTeamByTeamName(sysTeam.getTeamName());
        Preconditions.checkState(selectSysTeam==null,MessageUtils.message("team.insert_team_name_duplicate"));

        String createBy = SecurityUtils.getUserId().toString(); // 获取当前登陆用户id
        // 新建团队数据
        sysTeam.setCreateTime(DateUtils.getNowDate());
        sysTeam.setCreateBy(createBy);
        Preconditions.checkState(sysTeamMapper.insertSysTeam(sysTeam)==1, MessageUtils.message("team.insert_team_fail"));

        // 新建用户与团队关联数据
        SysUserTeamRole sysUserTeamRole = new SysUserTeamRole();
        sysUserTeamRole.setUserId(Long.valueOf(sysTeam.getCreateBy()));
        sysUserTeamRole.setTeamId(sysTeam.getTeamId());
        sysUserTeamRole.setTeamRoleId(DEFAULT_ROLE_ID);
        sysUserTeamRole.setCreateTime(DateUtils.getNowDate());
        sysUserTeamRole.setCreateBy(createBy);
        Preconditions.checkState(sysTeamMapper.insertSysUserTeamRole(sysUserTeamRole)==1,MessageUtils.message("team.insert_user_team_role_fail"));
        return 1;
    }

    /**
     * 修改团队
     * 
     * @param sysTeam 团队
     * @return 结果
     */
    @Override
    public int updateSysTeam(SysTeam sysTeam)
    {
        sysTeam.setUpdateTime(DateUtils.getNowDate());
        return sysTeamMapper.updateSysTeam(sysTeam);
    }

    /**
     * 批量删除团队
     * 
     * @param teamIds 需要删除的团队主键
     * @return 结果
     */
    @Override
    public int deleteSysTeamByTeamIds(Long[] teamIds)
    {
        return sysTeamMapper.deleteSysTeamByTeamIds(teamIds);
    }

    /**
     * 删除团队信息
     * 
     * @param teamId 团队主键
     * @return 结果
     */
    @Override
    public int deleteSysTeamByTeamId(Long teamId)
    {
        return sysTeamMapper.deleteSysTeamByTeamId(teamId);
    }
}
