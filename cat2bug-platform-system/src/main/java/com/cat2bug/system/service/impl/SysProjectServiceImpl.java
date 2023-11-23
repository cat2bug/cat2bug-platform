package com.cat2bug.system.service.impl;

import java.util.HashMap;
import java.util.List;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.domain.SysUserProjectRole;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.service.ISysProjectService;
import org.springframework.transaction.annotation.Transactional;

/**
 * 项目Service业务层处理
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
@Service
public class SysProjectServiceImpl implements ISysProjectService 
{
    @Autowired
    private SysProjectMapper sysProjectMapper;
    @Autowired
    private SysUserProjectMapper sysUserProjectMapper;
    @Autowired
    private SysUserProjectRoleMapper sysUserProjectRoleMapper;
    /**
     * 查询项目
     * 
     * @param projectId 项目主键
     * @return 项目
     */
    @Override
    public SysProject selectSysProjectByProjectId(Long projectId)
    {
        return sysProjectMapper.selectSysProjectByProjectId(projectId);
    }

    /**
     * 查询项目列表
     * 
     * @param sysProject 项目
     * @return 项目
     */
    @Override
    public List<SysProject> selectSysProjectList(SysProject sysProject)
    {
        Preconditions.checkNotNull(sysProject.getTeamId(), MessageUtils.message("project.team_cannot_empty"));
        // 设置当前用户可查看哪些项目
        if(sysProject.getParams()==null){
            sysProject.setParams(new HashMap<>());
        }
        sysProject.getParams().put("userId",SecurityUtils.getUserId());

        List<SysProject> projectList = sysProjectMapper.selectSysProjectList(sysProject);
        // 设置项目中的成员
        projectList.stream().forEach(p->{
            List<SysUser> members = sysUserProjectMapper.selectSysUserListByProjectId(p.getProjectId());
            p.setMembers(members);
        });
        return projectList;
    }

    /**
     * 新增项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    @Transactional
    @Override
    public int insertSysProject(SysProject sysProject)
    {
        // 新增项目
        sysProject.setCreateBy(String.valueOf(SecurityUtils.getUserId()));
        sysProject.setCreateTime(DateUtils.getNowDate());
        sysProject.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
        sysProject.setUpdateTime(DateUtils.getNowDate());
        Preconditions.checkState(sysProjectMapper.insertSysProject(sysProject)==1,MessageUtils.message("project.insert_project_fail"));

        // 新增项目成员
        sysProject.getMembers().stream().forEach(m->{
            SysUserProject up = new SysUserProject();
            up.setProjectId(sysProject.getProjectId());
            up.setUserId(m.getUserId());
            Preconditions.checkState(sysUserProjectMapper.insertSysUserProject(up)==1,MessageUtils.message("project.insert_user_fail"));
            // 新增成员角色
            for(Long roleId : m.getRoleIds()){
                SysUserProjectRole role = new SysUserProjectRole();
                role.setRoleId(roleId);
                role.setUserProjectId(up.getUserProjectId());
                Preconditions.checkState(sysUserProjectRoleMapper.insertSysUserProjectRole(role)==1,MessageUtils.message("project.insert_user_role_fail"));
            }
        });
        return 1;
    }

    /**
     * 修改项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    @Override
    public int updateSysProject(SysProject sysProject)
    {
        sysProject.setUpdateTime(DateUtils.getNowDate());
        return sysProjectMapper.updateSysProject(sysProject);
    }

    /**
     * 批量删除项目
     * 
     * @param projectIds 需要删除的项目主键
     * @return 结果
     */
    @Override
    public int deleteSysProjectByProjectIds(Long[] projectIds)
    {
        return sysProjectMapper.deleteSysProjectByProjectIds(projectIds);
    }

    /**
     * 删除项目信息
     * 
     * @param projectId 项目主键
     * @return 结果
     */
    @Override
    public int deleteSysProjectByProjectId(Long projectId)
    {
        return sysProjectMapper.deleteSysProjectByProjectId(projectId);
    }
}
