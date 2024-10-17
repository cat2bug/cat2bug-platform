package com.cat2bug.system.domain;

import java.util.List;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
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
@Data
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

    /** 执行子项总数 */
    private int itemTotal;

    /** 执行通过数量 */
    private int passCount;

    /** 执行未通过数量 */
    private int failCount;

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
