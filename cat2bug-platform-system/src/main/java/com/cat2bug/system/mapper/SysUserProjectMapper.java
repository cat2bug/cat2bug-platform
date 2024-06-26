package com.cat2bug.system.mapper;

import java.util.List;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysUserProject;
import org.apache.ibatis.annotations.Param;

/**
 * 用户项目Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-22
 */
public interface SysUserProjectMapper 
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

    /**
     * 查询用户列表
     * @param projectId 项目id
     * @return  用户集合
     */
    public List<SysUser> selectSysUserListByProjectId(@Param("projectId") Long projectId, @Param("sysUser") SysUser sysUser);

    /**
     * 查询非当前项目用户列表
     * @param projectId 项目id
     * @return  用户集合
     */
    public List<SysUser> selectNotSysUserListByProjectId(@Param("projectId") Long projectId, @Param("sysUser") SysUser sysUser);

    /**
     * 新增用户项目
     * 
     * @param sysUserProject 用户项目
     * @return 结果
     */
    public int insertSysUserProject(SysUserProject sysUserProject);

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
     * 删除用户项目
     * 
     * @param userProjectId 用户项目主键
     * @return 结果
     */
    public int deleteSysUserProjectByUserProjectId(Long userProjectId);

    /**
     * 批量删除用户项目
     * 
     * @param userProjectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserProjectByUserProjectIds(Long[] userProjectIds);
}
