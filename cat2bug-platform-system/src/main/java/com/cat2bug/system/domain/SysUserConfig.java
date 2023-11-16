package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 用户配置对象 sys_user_config
 * 
 * @author yuzhantao
 * @date 2023-11-16
 */
public class SysUserConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户配置id */
    private Long userConfigId;

    /** 当前团队id */
    @Excel(name = "当前团队id")
    private Long currentTeamId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    public void setUserConfigId(Long userConfigId) 
    {
        this.userConfigId = userConfigId;
    }

    public Long getUserConfigId() 
    {
        return userConfigId;
    }
    public void setCurrentTeamId(Long currentTeamId) 
    {
        this.currentTeamId = currentTeamId;
    }

    public Long getCurrentTeamId() 
    {
        return currentTeamId;
    }
    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userConfigId", getUserConfigId())
            .append("currentTeamId", getCurrentTeamId())
            .append("userId", getUserId())
            .toString();
    }
}
