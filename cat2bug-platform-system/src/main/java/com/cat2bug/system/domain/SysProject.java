package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

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

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 项目图标地址 */
    @Excel(name = "项目图标地址")
    private String projectIcon;

    /** 项目介绍 */
    @Excel(name = "项目介绍")
    private String projectIntroduce;

    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("projectId", getProjectId())
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
