package com.cat2bug.system.mapper;

import java.util.List;
import com.cat2bug.system.domain.SysUserProjectRole;
import org.apache.ibatis.annotations.Param;

/**
 * 用户项目角色Mapper接口
 * 
 * @author yuzhantao
 * @date 2023-11-22
 */
public interface SysUserProjectRoleMapper 
{
    /**
     * 查询用户项目角色
     * 
     * @param userProjectRoleId 用户项目角色主键
     * @return 用户项目角色
     */
    public SysUserProjectRole selectSysUserProjectRoleByUserProjectRoleId(Long userProjectRoleId);

    /**
     * 查询用户项目角色列表
     * 
     * @param sysUserProjectRole 用户项目角色
     * @return 用户项目角色集合
     */
    public List<SysUserProjectRole> selectSysUserProjectRoleList(SysUserProjectRole sysUserProjectRole);

    /**
     * 新增用户项目角色
     * 
     * @param sysUserProjectRole 用户项目角色
     * @return 结果
     */
    public int insertSysUserProjectRole(SysUserProjectRole sysUserProjectRole);

    /**
     * 修改用户项目角色
     * 
     * @param sysUserProjectRole 用户项目角色
     * @return 结果
     */
    public int updateSysUserProjectRole(SysUserProjectRole sysUserProjectRole);

    /**
     * 删除用户项目角色
     * 
     * @param userProjectRoleId 用户项目角色主键
     * @return 结果
     */
    public int deleteSysUserProjectRoleByUserProjectRoleId(Long userProjectRoleId);

    /**
     * 删除用户项目角色
     * @param userProjectId 用户项目id
     * @return
     */
    public int deleteSysUserProjectRoleByUserProjectId(Long userProjectId);
    /**
     * 批量删除用户项目角色
     * 
     * @param userProjectRoleIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSysUserProjectRoleByUserProjectRoleIds(Long[] userProjectRoleIds);
}
