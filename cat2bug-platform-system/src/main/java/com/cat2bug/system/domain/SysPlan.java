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

    /** 测试计划编号 */
    private Long planNumber;

    /** 测试计划名称 */
    @Excel(name = "测试计划名称")
    private String planName;

    /** 测试默认版本 */
    @Excel(name = "测试默认版本")
    private String planVersion;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "计划开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date planStartTime;

    /** 计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "计划结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
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
    /** 未执行数量 */
    private int unexecutedCount;

    /** 缺陷进行中状态统计数量 */
    private int processingStateCount;
    /** 缺陷待审核状态统计数量 */
    private int auditStateCount;
    /** 缺陷驳回状态统计数量 */
    private int rejectedStateCount;
    /** 缺陷已关闭状态统计数量 */
    private int closeStateCount;
    /** 缺陷等级严重数量 */
    private int defectLevelUrgentCount;
    /** 缺陷等级高数量 */
    private int defectLevelHeightCount;
    /** 缺陷等级中数量 */
    private int defectLevelMiddleCount;
    /** 缺陷等级低数量 */
    private int defectLevelLowCount;
    /** 缺陷数量 */
    private int defectCount;
    /** 交付物的数量 */
    private int moduleCount;
    /** 测试人员创建缺陷的数量 */
    private int createDefectCountByTester;
    /** 外部人员创建缺陷的数量 */
    private int createDefectCountByOutsider;
    /** 获取缺陷已执行数量 */
    public int getExecutedCount() {
        return this.itemTotal - this.unexecutedCount;
    }
    /** 获取缺陷发现率 */
    public String getDefectDiscoveryRate() {
        if(this.itemTotal==0) return "0%";
        return (int)Math.floor(this.defectCount*100/this.itemTotal) + "%";
    }
    /** 获取缺陷修复率 */
    public String getDefectRepairRate() {
        if(this.itemTotal==0) return "0%";
        return (int)Math.floor(this.closeStateCount*100/this.itemTotal) + "%";
    }
    /** 获取缺陷密度 */
    public String getDefectDensity() {
        if(this.moduleCount==0) return "0.0";
        return Math.floor(this.defectCount*100/this.moduleCount)+"";
    }
    /** 获取缺陷探测率 */
    public String getDefectDetectionRate() {
        if(this.defectCount==0) return "0%";
        return (int)Math.floor(this.createDefectCountByTester*100/this.defectCount) + "%";
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
