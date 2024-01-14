package com.cat2bug.system.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.*;
import com.cat2bug.system.domain.vo.BatchUserRoleVo;
import com.cat2bug.system.mapper.*;
import com.cat2bug.system.service.ISysUserConfigService;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    /** 创建人角色ID */
    private final static Long TEAM_CREATE_BY_ROLE_ID = 12L;

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
        // 处理排除用户的逻辑代码
        if(sysUser.getParams()!=null && sysUser.getParams().get("excludeMembers")!=null && sysUser.getParams().get("excludeMembers") instanceof String){
            String strExcludeMembers = String.valueOf(sysUser.getParams().get("excludeMembers"));
            sysUser.getParams().put("excludeMembers",strExcludeMembers.split(","));
        }
        return sysUserMapper.selectSysUserListByTeamIdAndSysUser(teamId, sysUser);
    }

    @Override
    public List<SysUser> selectSysUserListByTeamIdAndNotSysUser(Long teamId, SysUser sysUser) {
        return sysUserMapper.selectSysUserListByTeamIdAndNotSysUser(teamId, sysUser);
    }

    @Override
    public int inviteMember(BatchUserRoleVo batchUserRoleVo) {
        Preconditions.checkNotNull(batchUserRoleVo.getTeamId(),MessageUtils.message("team.team_not_empty"));
        Preconditions.checkArgument(batchUserRoleVo.getMemberIds()!=null && batchUserRoleVo.getMemberIds().length>0,MessageUtils.message("team.member_not_empty"));
        Preconditions.checkArgument(batchUserRoleVo.getRoleIds()!=null && batchUserRoleVo.getRoleIds().length>0,MessageUtils.message("team.role_not_empty"));
        for(Long memberId : batchUserRoleVo.getMemberIds()) {
            SysUserTeam sysUserTeam = new SysUserTeam();
            sysUserTeam.setUserId(memberId);
            sysUserTeam.setTeamId(batchUserRoleVo.getTeamId());
            sysUserTeam.setCreateTime(DateUtils.getNowDate());
            sysUserTeam.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
            Preconditions.checkState(sysUserTeamMapper.insertSysUserTeam(sysUserTeam)==1,MessageUtils.message("team.insert_user_team_role_fail"));

            // 新建团队内的用户角色
            if(batchUserRoleVo.getRoleIds()!=null){
                for(Long roleId : batchUserRoleVo.getRoleIds()){
                    SysUserTeamRole utr = new SysUserTeamRole();
                    utr.setUserTeamId(sysUserTeam.getUserTeamId());
                    utr.setRoleId(roleId);
                    Preconditions.checkState(sysUserTeamRoleMapper.insertSysUserTeamRole(utr)==1,MessageUtils.message("team.insert_member_role_fail"));
                }
            }
        }
        return batchUserRoleVo.getMemberIds().length;
    }

    /**
     * 新增团队
     * 
     * @param sysTeam 团队
     * @return 结果
     */
    @Override
    @Transactional
    public SysTeam insertSysTeam(SysTeam sysTeam)
    {
        Preconditions.checkNotNull(sysTeam.getTeamName(),MessageUtils.message("team.insert_team_name_cannot_empty"));

        // 检测团队名称是否已经使用
        SysTeam selectSysTeam = sysTeamMapper.selectSysTeamByTeamName(sysTeam.getTeamName());
        Preconditions.checkState(selectSysTeam==null,MessageUtils.message("team.insert_team_name_duplicate"));

        // 新建团队数据
        sysTeam.setCreateTime(DateUtils.getNowDate());
        sysTeam.setCreateBy(SecurityUtils.getUsername());
        sysTeam.setCreateById(SecurityUtils.getUserId());
        Preconditions.checkState(sysTeamMapper.insertSysTeam(sysTeam)==1, MessageUtils.message("team.insert_team_fail"));

        // 新建用户与团队关联数据
        String createById = SecurityUtils.getUserId().toString(); // 获取当前登陆用户id
        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(SecurityUtils.getUserId());
        sysUserTeam.setTeamId(sysTeam.getTeamId());
        sysUserTeam.setCreateTime(DateUtils.getNowDate());
        sysUserTeam.setCreateBy(createById);
        Preconditions.checkState(sysUserTeamMapper.insertSysUserTeam(sysUserTeam)==1,MessageUtils.message("team.insert_user_team_role_fail"));

        // 新建用户角色
        SysUserTeamRole sysUserTeamRole = new SysUserTeamRole();
        sysUserTeamRole.setRoleId(TEAM_CREATE_BY_ROLE_ID);
        sysUserTeamRole.setUserTeamId(sysUserTeam.getUserTeamId());
        Preconditions.checkState(sysUserTeamRoleMapper.insertSysUserTeamRole(sysUserTeamRole)==1,MessageUtils.message("team.insert_user_team_role_fail"));

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
        return sysTeam;
    }

    @Override
    @Transactional
    public int insertSysUser(Long teamId, SysUser user) {
        // 插入用户信息
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        Preconditions.checkState(sysUserMapper.insertUser(user)==1,MessageUtils.message("user.insert_user_fail"));

        // 新建用户与团队关联数据
        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(user.getUserId());
        sysUserTeam.setTeamId(teamId);
        sysUserTeam.setCreateTime(DateUtils.getNowDate());
        sysUserTeam.setCreateBy(String.valueOf(SecurityUtils.getUsername()));
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
        // 新建用户配置
        SysUserConfig sysUserConfig = new SysUserConfig();
        sysUserConfig.setUserId(user.getUserId());
        sysUserConfig.setCurrentTeamId(teamId);
        Preconditions.checkState(sysUserConfigMapper.insertSysUserConfig(sysUserConfig)==1,MessageUtils.message("user.insert_user_config_fail"));

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
