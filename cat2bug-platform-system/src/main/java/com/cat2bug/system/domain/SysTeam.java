package com.cat2bug.system.domain;

import lombok.Data;
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
@Data
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
    private boolean isDel;

    /** 项目数量 */
    @Excel(name = "项目数量")
    private int projectCount;

    /** 项目数量 */
    @Excel(name = "成员数量")
    private int memberCount;

    /** 是否锁定 */
    @Excel(name = "是否锁定")
    private Boolean lock;

    /** 锁定备注 */
    @Excel(name = "锁定备注")
    private String lockRemark;

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
            .append("isDel", isDel())
            .append("projectCount", getProjectCount())
            .toString();
    }
}
