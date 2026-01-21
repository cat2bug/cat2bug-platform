package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.type.SysProjectStateEnum;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 项目对象 sys_project
 * 
 * @author yuzhantao
 * @date 2023-11-13
 */
public class SysProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目id */
    private Long projectId;

    /** 团队id */
    private Long teamId;

    /** 团队名称 */
    private String teamName;

    /** 团队图标 */
    private String teamIcon;

    /** 团队状态 */
    private SysProjectStateEnum projectState;

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 项目图标地址 */
    @Excel(name = "项目图标地址")
    private String projectIcon;

    /** 项目介绍 */
    @Excel(name = "项目介绍")
    private String projectIntroduce;
    /** 是否收藏 */
    private boolean collect;
    /** 成员集合 */
    private List<SysUser> members;

    /** 是否锁定 */
    @Excel(name = "是否锁定")
    private Boolean lock;

    /** 锁定备注 */
    @Excel(name = "锁定备注")
    private String lockRemark;

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getProjectName() 
    {
        return projectName;
    }
    public void setProjectIcon(String projectIcon) 
    {
        this.projectIcon = projectIcon;
    }

    public String getProjectIcon() 
    {
        return projectIcon;
    }
    public void setProjectIntroduce(String projectIntroduce) 
    {
        this.projectIntroduce = projectIntroduce;
    }

    public String getProjectIntroduce() 
    {
        return projectIntroduce;
    }

    public List<SysUser> getMembers() {
        return members;
    }

    public void setMembers(List<SysUser> members) {
        this.members = members;
    }

    public boolean isCollect() {
        return collect;
    }

    public void setCollect(boolean collect) {
        this.collect = collect;
    }

    public SysProjectStateEnum getProjectState() {
        return projectState;
    }

    public void setProjectState(SysProjectStateEnum projectState) {
        this.projectState = projectState;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamIcon() {
        return teamIcon;
    }

    public void setTeamIcon(String teamIcon) {
        this.teamIcon = teamIcon;
    }

    public Boolean getLock() {
        return lock;
    }

    public void setLock(Boolean lock) {
        this.lock = lock;
    }

    public String getLockRemark() {
        return lockRemark;
    }

    public void setLockRemark(String lockRemark) {
        this.lockRemark = lockRemark;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("projectId", getProjectId())
            .append("teamId", getTeamId())
            .append("projectName", getProjectName())
            .append("projectIcon", getProjectIcon())
            .append("projectIntroduce", getProjectIntroduce())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
