package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 用户项目对象 sys_user_project
 * 
 * @author yuzhantao
 * @date 2023-11-22
 */
public class SysUserProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户项目id */
    private Long userProjectId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 项目id */
    @Excel(name = "项目id")
    private Long projectId;

    /** 是否锁定 */
    @Excel(name = "是否锁定")
    private Integer projectLock;

    /** 是否收藏 */
    private Boolean collect;

    public void setUserProjectId(Long userProjectId) 
    {
        this.userProjectId = userProjectId;
    }

    public Long getUserProjectId() 
    {
        return userProjectId;
    }
    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }
    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }
    public void setProjectLock(Integer projectLock) 
    {
        this.projectLock = projectLock;
    }

    public Integer getProjectLock() 
    {
        return projectLock;
    }

    public Boolean getCollect() {
        return collect;
    }

    public void setCollect(Boolean collect) {
        this.collect = collect;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("collect", getCollect())
             .append("userProjectId", getUserProjectId())
            .append("userId", getUserId())
            .append("projectId", getProjectId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("projectLock", getProjectLock())
            .toString();
    }
}
