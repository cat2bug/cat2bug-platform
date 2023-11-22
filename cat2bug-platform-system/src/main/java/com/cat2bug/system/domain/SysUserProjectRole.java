package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 用户项目角色对象 sys_user_project_role
 * 
 * @author yuzhantao
 * @date 2023-11-22
 */
public class SysUserProjectRole extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户项目角色id */
    private Long userProjectRoleId;

    /** 用户项目id */
    @Excel(name = "用户项目id")
    private Long userProjectId;

    /** 角色id */
    @Excel(name = "角色id")
    private Long roleId;

    public void setUserProjectRoleId(Long userProjectRoleId) 
    {
        this.userProjectRoleId = userProjectRoleId;
    }

    public Long getUserProjectRoleId() 
    {
        return userProjectRoleId;
    }
    public void setUserProjectId(Long userProjectId) 
    {
        this.userProjectId = userProjectId;
    }

    public Long getUserProjectId() 
    {
        return userProjectId;
    }
    public void setRoleId(Long roleId) 
    {
        this.roleId = roleId;
    }

    public Long getRoleId() 
    {
        return roleId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userProjectRoleId", getUserProjectRoleId())
            .append("userProjectId", getUserProjectId())
            .append("roleId", getRoleId())
            .toString();
    }
}
