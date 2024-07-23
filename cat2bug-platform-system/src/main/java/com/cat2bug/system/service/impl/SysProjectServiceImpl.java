package com.cat2bug.system.service.impl;

import java.util.*;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.*;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectRoleMapper;
import com.cat2bug.system.service.ISysProjectDefectTabsService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysProjectMapper;
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
    @Autowired
    private ISysUserConfigService sysUserConfigService;
    @Autowired
    private ISysProjectDefectTabsService sysProjectDefectTabsService;
    /**
     * 查询项目
     * 
     * @param projectId 项目主键
     * @return 项目
     */
    @Override
    public SysProject selectSysProjectByProjectId(Long projectId)
    {
        return sysProjectMapper.selectSysProjectByProjectId(projectId,SecurityUtils.getUserId());
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
//        // 设置当前用户可查看哪些项目
//        if(sysProject.getParams()==null){
//            sysProject.setParams(new HashMap<>());
//        }
//        sysProject.getParams().put("userId",SecurityUtils.getUserId());

        List<SysProject> projectList = sysProjectMapper.selectSysProjectList(SecurityUtils.getUserId(), sysProject);
        // 设置项目中的成员
        projectList.stream().forEach(p->{
            List<SysUser> members = sysUserProjectMapper.selectSysUserListByProjectId(p.getProjectId(),new SysUser());
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
            // 添加缺陷tab配置
            SysProjectDefectTabs tab1 = new SysProjectDefectTabs();
            tab1.setUserId(up.getUserId());
            tab1.setProjectId(up.getProjectId());
            tab1.setTabName(MessageUtils.message("my"));
            SysDefect sysDefect1 = new SysDefect();
            sysDefect1.setHandleBy(Arrays.asList(up.getUserId()));
            tab1.setConfig(sysDefect1);
            sysProjectDefectTabsService.insertSysProjectDefectTabs(tab1);
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
    @Transactional
    public int deleteSysProjectByProjectId(Long projectId)
    {
        int ret = sysProjectMapper.deleteSysProjectByProjectId(projectId);
        SysUserConfig sysUserConfig = sysUserConfigService.selectSysUserConfigByCurrentUserId();
        // 如果删除的项目是当前用户项目，就选择另外一个项目作为当前项目
        if(sysUserConfig.getCurrentProjectId()==projectId) {
            // 查找当前用户在当前团队中，有哪些项目可操作
            SysProject sysProject = new SysProject();
            Map<String,Object> params = new HashMap<>();
            params.put("userId",SecurityUtils.getUserId());
            sysProject.setParams(params);
            sysProject.setTeamId(sysUserConfig.getCurrentTeamId());
            List<SysProject> projectList = sysProjectMapper.selectSysProjectList(SecurityUtils.getUserId(), sysProject);

            // 将查到的第一个非projectId项目作为当前默认项目
            Optional<SysProject> potProject = projectList.stream().filter(p->p.getProjectId()!=projectId).findFirst();
            if(potProject.isPresent()){
                sysUserConfig.setCurrentProjectId(potProject.get().getProjectId());
            } else {
                sysUserConfig.setCurrentProjectId(0L);
            }
            sysUserConfigService.updateSysUserConfig(sysUserConfig);
        }
        return ret;
    }
}
