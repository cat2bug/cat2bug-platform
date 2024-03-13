package com.cat2bug.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 报告对象 sys_report
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
public class SysReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 报告ID */
    private Long reportId;

    /** 报告标题 */
    @Excel(name = "报告标题")
    private String reportTitle;

    /** 报告时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "报告时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date reportTime;

    /** 报告描述 */
    @Excel(name = "报告描述")
    private String reportDescription;

    /** 数据类型 */
    @Excel(name = "数据类型")
    private String reportDataType;

    /** 数据 */
    @Excel(name = "数据")
    private String reportData;

    /**  推送人ID */
    @Excel(name = " 推送人ID")
    private Long createById;

    public void setReportId(Long reportId) 
    {
        this.reportId = reportId;
    }

    public Long getReportId() 
    {
        return reportId;
    }
    public void setReportTitle(String reportTitle) 
    {
        this.reportTitle = reportTitle;
    }

    public String getReportTitle() 
    {
        return reportTitle;
    }
    public void setReportTime(Date reportTime) 
    {
        this.reportTime = reportTime;
    }

    public Date getReportTime() 
    {
        return reportTime;
    }
    public void setReportDescription(String reportDescription) 
    {
        this.reportDescription = reportDescription;
    }

    public String getReportDescription() 
    {
        return reportDescription;
    }
    public void setReportDataType(String reportDataType) 
    {
        this.reportDataType = reportDataType;
    }

    public String getReportDataType() 
    {
        return reportDataType;
    }
    public void setReportData(String reportData) 
    {
        this.reportData = reportData;
    }

    public String getReportData() 
    {
        return reportData;
    }
    public void setCreateById(Long createById) 
    {
        this.createById = createById;
    }

    public Long getCreateById() 
    {
        return createById;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reportId", getReportId())
            .append("reportTitle", getReportTitle())
            .append("reportTime", getReportTime())
            .append("reportDescription", getReportDescription())
            .append("reportDataType", getReportDataType())
            .append("reportData", getReportData())
            .append("createById", getCreateById())
            .toString();
    }
}
