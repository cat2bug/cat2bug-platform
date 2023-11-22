package com.cat2bug.system.service.impl;

import java.util.List;
import com.cat2bug.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.service.ISysUserProjectService;

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
