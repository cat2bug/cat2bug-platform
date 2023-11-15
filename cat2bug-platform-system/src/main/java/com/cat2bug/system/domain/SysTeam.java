package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 团队对象 sys_team
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
public class SysTeam extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 团队id */
    private Long teamId;

    /** 团队名称 */
    @Excel(name = "团队名称")
    private String teamName;

    /** 团队图标 */
    @Excel(name = "团队图标")
    private String teamIcon;

    /** 团队介绍 */
    @Excel(name = "团队介绍")
    private String introduce;

    /** 是否删除 */
    private String isDel;

    public void setTeamId(Long teamId) 
    {
        this.teamId = teamId;
    }

    public Long getTeamId() 
    {
        return teamId;
    }
    public void setTeamName(String teamName) 
    {
        this.teamName = teamName;
    }

    public String getTeamName() 
    {
        return teamName;
    }
    public void setTeamIcon(String teamIcon) 
    {
        this.teamIcon = teamIcon;
    }

    public String getTeamIcon() 
    {
        return teamIcon;
    }
    public void setIntroduce(String introduce) 
    {
        this.introduce = introduce;
    }

    public String getIntroduce() 
    {
        return introduce;
    }
    public void setIsDel(String isDel) 
    {
        this.isDel = isDel;
    }

    public String getIsDel() 
    {
        return isDel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("teamId", getTeamId())
            .append("teamName", getTeamName())
            .append("teamIcon", getTeamIcon())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("introduce", getIntroduce())
            .append("isDel", getIsDel())
            .toString();
    }
}
