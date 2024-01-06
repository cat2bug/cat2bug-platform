package com.cat2bug.system.service.impl;

import java.util.List;
import java.util.Optional;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysUserProjectRole;
import com.cat2bug.system.domain.vo.BatchUserRoleVo;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.service.ISysUserProjectService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户项目Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-22
 */
@Service
public class SysUserProjectServiceImpl implements ISysUserProjectService 
{
    @Autowired
    private SysUserProjectMapper sysUserProjectMapper;

    @Autowired
    private SysUserProjectRoleMapper sysUserProjectRoleMapper;

    /**
     * 查询用户项目
     * 
     * @param userProjectId 用户项目主键
     * @return 用户项目
     */
    @Override
    public SysUserProject selectSysUserProjectByUserProjectId(Long userProjectId)
    {
        return sysUserProjectMapper.selectSysUserProjectByUserProjectId(userProjectId);
    }

    /**
     * 查询用户项目列表
     * 
     * @param sysUserProject 用户项目
     * @return 用户项目
     */
    @Override
    public List<SysUserProject> selectSysUserProjectList(SysUserProject sysUserProject)
    {
        return sysUserProjectMapper.selectSysUserProjectList(sysUserProject);
    }

    @Override
    public List<SysUser> selectSysUserListByProjectId(Long projectId, SysUser sysUser) {
        return sysUserProjectMapper.selectSysUserListByProjectId(projectId, sysUser);
    }

    @Override
    public List<SysUser> selectNotSysUserListByProjectId(Long projectId, SysUser sysUser) {
        return sysUserProjectMapper.selectNotSysUserListByProjectId(projectId, sysUser);
    }

    /**
     * 新增用户项目
     * 
     * @param sysUserProject 用户项目
     * @return 结果
     */
    @Override
    public int insertSysUserProject(SysUserProject sysUserProject)
    {
        sysUserProject.setCreateTime(DateUtils.getNowDate());
        return sysUserProjectMapper.insertSysUserProject(sysUserProject);
    }

    @Override
    @Transactional
    public int batchInsertSysUserProject(BatchUserRoleVo batchUserRoleVo) {
        int ret = 0;
        for(int i=0;i<batchUserRoleVo.getMemberIds().length;i++){
            // 添加用户项目数据
            SysUserProject up =new SysUserProject();
            up.setProjectId(batchUserRoleVo.getProjectId());
            up.setUserId(batchUserRoleVo.getMemberIds()[i]);
            ret += sysUserProjectMapper.insertSysUserProject(up);

            // 添加用户项目权限数据
            SysUserProjectRole upr =new SysUserProjectRole();
            upr.setUserProjectId(up.getUserProjectId());
            for(int j=0;j<batchUserRoleVo.getRoleIds().length;j++) {
                upr.setRoleId(batchUserRoleVo.getRoleIds()[j]);
                int r = sysUserProjectRoleMapper.insertSysUserProjectRole(upr);
                Preconditions.checkState(r>0, MessageUtils.message("project.add_member_fail"));
            }
            Preconditions.checkState(ret>i, MessageUtils.message("project.add_member_fail"));
        }
        return ret;
    }

    /**
     * 修改用户项目
     * 
     * @param sysUserProject 用户项目
     * @return 结果
     */
    @Override
    public int updateSysUserProject(SysUserProject sysUserProject)
    {
        sysUserProject.setUpdateTime(DateUtils.getNowDate());
        return sysUserProjectMapper.updateSysUserProject(sysUserProject);
    }

    @Override
    public int updateSysUserProjectByUserIdAndProjectId(SysUserProject sysUserProject) {
        return sysUserProjectMapper.updateSysUserProjectByUserIdAndProjectId(sysUserProject);
    }

    /**
     * 批量删除用户项目
     * 
     * @param userProjectIds 需要删除的用户项目主键
     * @return 结果
     */
    @Override
    public int deleteSysUserProjectByUserProjectIds(Long[] userProjectIds)
    {
        return sysUserProjectMapper.deleteSysUserProjectByUserProjectIds(userProjectIds);
    }

    /**
     * 删除用户项目信息
     * @param projectId 项目id
     * @param memberId  成员id
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSysUserProjectByProjectIdAndMemberId(Long projectId, Long memberId) {
        SysUserProject up = new SysUserProject();
        up.setUserId(memberId);
        up.setProjectId(projectId);
        Optional<SysUserProject> sysUserProject = sysUserProjectMapper.selectSysUserProjectList(up).stream().findFirst();
        Preconditions.checkState(sysUserProject.isPresent(),MessageUtils.message("project.member_not_found"));
        Long sysUserProjectId = sysUserProject.get().getUserProjectId();
        int ret = sysUserProjectMapper.deleteSysUserProjectByUserProjectId(sysUserProjectId);
        sysUserProjectRoleMapper.deleteSysUserProjectRoleByUserProjectId(sysUserProjectId);
        return ret;
    }

    /**
     * 删除用户项目信息
     * 
     * @param userProjectId 用户项目主键
     * @return 结果
     */
    @Override
    public int deleteSysUserProjectByUserProjectId(Long userProjectId)
    {
        return sysUserProjectMapper.deleteSysUserProjectByUserProjectId(userProjectId);
    }
}
