package com.cat2bug.system.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.domain.SysUserTeam;
import com.cat2bug.system.domain.SysUserTeamRole;
import com.cat2bug.system.mapper.*;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserConfigMapper sysUserConfigMapper;

    @Autowired
    private SysUserTeamMapper sysUserTeamMapper;

    @Autowired
    private SysUserTeamRoleMapper sysUserTeamRoleMapper;
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
     * @return 团队集合
     */
    @Override
    public List<SysTeam> selectSysTeamList(SysTeam sysTeam)
    {
        return sysTeamMapper.selectSysTeamList(sysTeam);
    }

    /**
     * 查询团队列表
     * @param userId    用户id
     * @return  团队集合
     */
    @Override
    public List<SysTeam> selectSysTeamListByUserId(Long userId) {
        return sysTeamMapper.selectSysTeamListByUserId(userId);
    }

    /**
     * 查询团队列表
     * @param teamId    团队id
     * @return          成员集合
     */
    @Override
    public List<SysUser> selectSysUserListByTeamIdAndSysUser(Long teamId, SysUser sysUser) {
        return sysUserMapper.selectSysUserListByTeamIdAndSysUser(teamId, sysUser);
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
        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(Long.valueOf(sysTeam.getCreateBy()));
        sysUserTeam.setTeamId(sysTeam.getTeamId());
        sysUserTeam.setCreateTime(DateUtils.getNowDate());
        sysUserTeam.setCreateBy(createBy);
        Preconditions.checkState(sysTeamMapper.insertSysUserTeam(sysUserTeam)==1,MessageUtils.message("team.insert_user_team_role_fail"));

        // TODO role
//        sysUserTeam.setTeamRoleId(DEFAULT_ROLE_ID);

        // 查询用户配置，如果没有默认团队，就将当前团队设置为默认团队
        SysUserConfig sysUserConfig = sysUserConfigMapper.selectSysUserConfigByUserId(SecurityUtils.getUserId());
        if(sysUserConfig==null){
            sysUserConfig = new SysUserConfig();
            sysUserConfig.setCurrentTeamId(sysTeam.getTeamId());
            sysUserConfig.setUserId(SecurityUtils.getUserId());
            sysUserConfigMapper.insertSysUserConfig(sysUserConfig);
        } else if(sysUserConfig.getCurrentTeamId()==null){
            // 设置当前团队id，并更新数据库
            sysUserConfig.setCurrentTeamId(sysTeam.getTeamId());
            sysUserConfigMapper.updateSysUserConfig(sysUserConfig);
        }
        return 1;
    }

    @Override
    @Transactional
    public int insertSysUser(Long teamId, SysUser user) {
        // 插入用户信息
        Preconditions.checkState(sysUserMapper.insertUser(user)==1,MessageUtils.message("user.insert_user_fail"));

        // 新建用户与团队关联数据
        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(user.getUserId());
        sysUserTeam.setTeamId(teamId);
        sysUserTeam.setCreateTime(DateUtils.getNowDate());
        sysUserTeam.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
        Preconditions.checkState(sysUserTeamMapper.insertSysUserTeam(sysUserTeam)==1,MessageUtils.message("team.insert_user_team_role_fail"));

        // 新建团队内的用户角色
        if(user.getRoleIds()!=null){
            for(Long roleId : user.getRoleIds()){
                SysUserTeamRole utr = new SysUserTeamRole();
                utr.setUserTeamId(sysUserTeam.getUserTeamId());
                utr.setRoleId(roleId);
                Preconditions.checkState(sysUserTeamRoleMapper.insertSysUserTeamRole(utr)==1,MessageUtils.message("team.insert_member_role_fail"));
            }
        }

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
