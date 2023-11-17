package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.service.ISysProjectService;

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
        return sysProjectMapper.selectSysProjectList(sysProject);
    }

    /**
     * 新增项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    @Override
    public int insertSysProject(SysProject sysProject)
    {
        sysProject.setCreateTime(DateUtils.getNowDate());
        return sysProjectMapper.insertSysProject(sysProject);
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
