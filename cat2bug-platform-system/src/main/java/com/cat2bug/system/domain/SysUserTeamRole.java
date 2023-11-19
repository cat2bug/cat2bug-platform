package com.cat2bug.system.domain;

import lombok.Data;
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
@Data
public class SysUserTeamRole extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户团队id */
    private Long userTeamId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 团队id */
    @Excel(name = "团队id")
    private Long teamId;

    /** 团队角色id */
    @Excel(name = "团队角色id")
    private Long teamRoleId;

    /** 是否锁定 */
    @Excel(name = "是否锁定")
    private Integer teamLock;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userTeamId", getUserTeamId())
            .append("userId", getUserId())
            .append("teamId", getTeamId())
            .append("teamRoleId", getTeamRoleId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("teamLock", getTeamLock())
            .toString();
    }
}
