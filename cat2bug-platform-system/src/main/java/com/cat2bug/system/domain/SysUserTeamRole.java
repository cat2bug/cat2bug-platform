package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-15 14:32
 * @Version: 1.0.0
 * 用户团队角色
 */
@Data
public class SysUserTeamRole extends BaseEntity {
    /** 表id */
    private Long userTeamId;
    /** 表id */
    private Long userId;
    /** 表id */
    private Long teamId;
    /** 表id */
    private Long teamRoleId;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("userTeamId", getUserTeamId())
                .append("userId", getUserId())
                .append("teamId", getTeamId())
                .append("teamRoleId", getTeamRoleId())
                .toString();
    }
}
