package com.cat2bug.system.service;

import java.util.List;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.domain.vo.BatchUserRoleVo;

/**
 * 用户项目Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-22
 */
public interface ISysUserProjectService 
{
    /**
     * 查询用户项目
     * 
     * @param userProjectId 用户项目主键
     * @return 用户项目
     */
    public SysUserProject selectSysUserProjectByUserProjectId(Long userProjectId);

    /**
     * 查询用户项目列表
     * 
     * @param sysUserProject 用户项目
     * @return 用户项目集合
     */
    public List<SysUserProject> selectSysUserProjectList(SysUserProject sysUserProject);

    public List<SysUser> selectSysUserListByProjectId(Long projectId, SysUser sysUser);

    public List<SysUser> selectNotSysUserListByProjectId(Long projectId, SysUser sysUser);

    /**
     * 新增用户项目
     * 
     * @param sysUserProject 用户项目
     * @return 结果
     */
    public int insertSysUserProject(SysUserProject sysUserProject);

    /**
     * 批量新增用户项目
     *
     * @param sysUserProjectList 用户项目集合
     * @return 结果
     */
    public int batchInsertSysUserProject(BatchUserRoleVo batchUserRoleVo);

    /**
     * 修改用户项目
     * 
     * @param sysUserProject 用户项目
     * @return 结果
     */
    public int updateSysUserProject(SysUserProject sysUserProject);

    /**
     * 修改用户项目
     *
     * @param sysUserProject 用户项目
     * @return 结果
     */
    public int updateSysUserProjectByUserIdAndProjectId(SysUserProject sysUserProject);

    /**
     * 批量删除用户项目
     * 
     * @param userProjectIds 需要删除的用户项目主键集合
     * @return 结果
     */
    public int deleteSysUserProjectByUserProjectIds(Long[] userProjectIds);

    /**
     * 删除用户项目信息
     * @param projectId 项目id
     * @param memberId  成员id
     * @return
     */
    public int deleteSysUserProjectByProjectIdAndMemberId(Long projectId, Long memberId);
    /**
     * 删除用户项目信息
     * 
     * @param userProjectId 用户项目主键
     * @return 结果
     */
    public int deleteSysUserProjectByUserProjectId(Long userProjectId);
}
