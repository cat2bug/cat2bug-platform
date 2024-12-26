package com.cat2bug.system.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import com.cat2bug.system.domain.SysProject;

/**
 * 项目Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
public interface ISysProjectService 
{
    /**
     * 查询项目
     * 
     * @param projectId 项目主键
     * @return 项目
     */
    public SysProject selectSysProjectByProjectId(Long projectId);

    /**
     * 查询项目列表
     * 
     * @param sysProject 项目
     * @return 项目集合
     */
    public List<SysProject> selectSysProjectList(SysProject sysProject);

    /**
     * 新增项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    public int insertSysProject(SysProject sysProject);

    /**
     * 修改项目
     * 
     * @param sysProject 项目
     * @return 结果
     */
    public int updateSysProject(SysProject sysProject);

    /**
     * 批量删除项目
     * 
     * @param projectIds 需要删除的项目主键集合
     * @return 结果
     */
    public int deleteSysProjectByProjectIds(Long[] projectIds);

    /**
     * 删除项目信息
     * 
     * @param projectId 项目主键
     * @return 结果
     */
    public int deleteSysProjectByProjectId(Long projectId);

    /**
     * 推送项目数据到云平台
     * @param projectId     项目ID
     * @param pullKey       推送KEY
     * @return
     */
    public boolean pullToCloud(Long projectId, String pullKey) throws IOException;
}
