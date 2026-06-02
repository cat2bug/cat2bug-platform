package com.cat2bug.system.service;

import com.cat2bug.system.domain.SysColumnsInChart;
import com.cat2bug.system.domain.SysDefectOpenWorkload;
import com.cat2bug.system.domain.SysDefectOpenWorkloadSummary;
import com.cat2bug.system.domain.SysDefectParticipationDay;
import com.cat2bug.system.domain.SysPlanCountdownSummary;
import com.cat2bug.system.domain.SysPlanMetricsItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 缺陷统计服务
 */
public interface ISysDefectStatisticService {
    /**
     * 按照缺陷类型统计
     * @param projectId 项目id
     * @param memberId  成员id
     * @return
     */
    public List<Map<String,Object>> typeStatistic(Long projectId, Long memberId);
    /**
     * 按照缺陷状态统计
     * @param projectId 项目id
     * @param memberId  成员id
     * @return
     */
    public List<Map<String,Object>> stateStatistic(Long projectId, Long memberId);

    /**
     * 按照模块统计
     * @param projectId
     * @return
     */
    public List<Map<String, Object>> moduleStatistic(@Param("projectId") Long projectId);

    /**
     * 团队未关闭待办负载 Top5
     */
    List<SysDefectOpenWorkload> openWorkload(Long projectId);

    /**
     * 个人未关闭待办汇总
     */
    SysDefectOpenWorkloadSummary openWorkloadMy(Long projectId, Long userId);

    /**
     * 个人按日缺陷日志参与统计（补齐缺失日期为 0）
     */
    List<SysDefectParticipationDay> participationMy(Long projectId, Long userId, int days);

    /**
     * 项目测试计划轻量列表
     */
    List<Map<String, Object>> planList(Long projectId);

    /**
     * 测试计划燃尽图（含日期轴补齐）
     */
    List<SysColumnsInChart> planBurndownChart(String planId);

    /**
     * 项目测试计划质量指标（雷达图）
     */
    List<SysPlanMetricsItem> planMetrics(Long projectId);

    /**
     * 测试计划倒计时摘要（缺陷统计域）
     */
    SysPlanCountdownSummary planCountdownSummary(String planId);

    /**
     * 缺陷状态走势（与仪表盘 defect-line 一致）
     */
    Map<String, Object> defectStateLine(Long projectId, String timeType);

    /**
     * 成员处理缺陷走势（与仪表盘 member-defect-line 一致，含 userId）
     */
    Map<String, Object> memberDefectLine(Long projectId, String timeType);
}
