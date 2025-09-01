package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 用户团队角色对象 sys_user_team_role
 * 
 * @author yuzhantao
 * @date 2023-11-20
 */
public class SysUserTeamRole extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户团队角色id */
    private Long userTeamRoleId;

    /** 用户团队id */
    @Excel(name = "用户团队id")
    private Long userTeamId;

    /** 角色id */
    @Excel(name = "角色id")
    private Long roleId;

    public void setUserTeamRoleId(Long userTeamRoleId) 
    {
        this.userTeamRoleId = userTeamRoleId;
    }

    public Long getUserTeamRoleId() 
    {
        return userTeamRoleId;
    }
    public void setUserTeamId(Long userTeamId) 
    {
        this.userTeamId = userTeamId;
    }

    public Long getUserTeamId() 
    {
        return userTeamId;
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
            .append("userTeamRoleId", getUserTeamRoleId())
            .append("userTeamId", getUserTeamId())
            .append("roleId", getRoleId())
            .toString();
    }
}
