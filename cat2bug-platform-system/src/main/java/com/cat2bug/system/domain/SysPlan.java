package com.cat2bug.system.domain;

import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 测试计划对象 sys_plan
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
public class SysPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 测试计划ID */
    private String planId;

    /** 测试计划名称 */
    @Excel(name = "测试计划名称")
    private String planName;

    /** 测试默认版本 */
    @Excel(name = "测试默认版本")
    private String planVersion;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date planStartTime;

    /** 计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date planEndTime;

    /** 创建人ID */
    private Long createById;

    /** 更新人ID */
    @Excel(name = "更新人ID")
    private Long updateById;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 报告ID */
    @Excel(name = "报告ID")
    private Long reportId;

    /** 测试计划子项信息 */
    private List<SysPlanItem> sysPlanItemList;

    public void setPlanId(String planId) 
    {
        this.planId = planId;
    }

    public String getPlanId() 
    {
        return planId;
    }
    public void setPlanName(String planName) 
    {
        this.planName = planName;
    }

    public String getPlanName() 
    {
        return planName;
    }
    public void setPlanVersion(String planVersion) 
    {
        this.planVersion = planVersion;
    }

    public String getPlanVersion() 
    {
        return planVersion;
    }
    public void setPlanStartTime(Date planStartTime) 
    {
        this.planStartTime = planStartTime;
    }

    public Date getPlanStartTime() 
    {
        return planStartTime;
    }
    public void setPlanEndTime(Date planEndTime) 
    {
        this.planEndTime = planEndTime;
    }

    public Date getPlanEndTime() 
    {
        return planEndTime;
    }
    public void setCreateById(Long createById) 
    {
        this.createById = createById;
    }

    public Long getCreateById() 
    {
        return createById;
    }
    public void setUpdateById(Long updateById) 
    {
        this.updateById = updateById;
    }

    public Long getUpdateById() 
    {
        return updateById;
    }
    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }
    public void setReportId(Long reportId) 
    {
        this.reportId = reportId;
    }

    public Long getReportId() 
    {
        return reportId;
    }

    public List<SysPlanItem> getSysPlanItemList()
    {
        return sysPlanItemList;
    }

    public void setSysPlanItemList(List<SysPlanItem> sysPlanItemList)
    {
        this.sysPlanItemList = sysPlanItemList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("planId", getPlanId())
            .append("planName", getPlanName())
            .append("planVersion", getPlanVersion())
            .append("planStartTime", getPlanStartTime())
            .append("planEndTime", getPlanEndTime())
            .append("createById", getCreateById())
            .append("createTime", getCreateTime())
            .append("updateById", getUpdateById())
            .append("updateTime", getUpdateTime())
            .append("projectId", getProjectId())
            .append("reportId", getReportId())
            .append("sysPlanItemList", getSysPlanItemList())
            .toString();
    }
}
