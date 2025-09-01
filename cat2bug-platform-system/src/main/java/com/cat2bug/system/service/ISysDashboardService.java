package com.cat2bug.system.service;

import com.cat2bug.system.domain.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仪表盘服务
 */
public interface ISysDashboardService {

    /**
     * 测试用例统计
     * @param projectId
     * @return
     */
    public SysCaseStatistics caseStatistics(Long projectId);
    /**
     * 测试用例统计
     * @param projectId
     * @return
     */
    public SysDefectStatistics defectStatistics(Long projectId);
    /**
     * 交付物统计
     * @param projectId
     * @return
     */
    public SysModuleStatistics moduleStatistics(Long projectId);
    /**
     * 报告统计
     * @param projectId
     * @return
     */
    public SysReportStatistics reportStatistics(Long projectId);
    /**
     * 文档统计
     * @param projectId
     * @return
     */
    public SysDocumentStatistics documentStatistics(Long projectId);

    /**
     * 成员统计
     * @param projectId
     * @return
     */
    public SysMemberStatistics memberStatistics(Long projectId);
    /**
     * 获取活动列表
     * @param projectId 项目ID
     * @param type      活动类型
     * @return          集合
     */
    public List<SysAction> actonList(Long projectId, String type);
    /**
     * 缺陷折线图数据
     * @param projectId 项目ID
     * @param timeType  时间类型 day, month
     * @return          集合
     */
    public List<SysDefectLine> defectLine(Long projectId, String timeType);

    /**
     * 测试计划燃烧图
     * @param planId    计划ID
     * @return          集合
     */
    public List<SysColumnsInChart> planBurndown(String planId);

    /**
     * 成员缺陷处理排行
     * @param projectId
     * @return
     */
    public List<SysMemberRankOfDefects> memberRankOfDefects(Long projectId);

    /**
     * 用户处理缺陷折线图
     * @param projectId
     * @param timeType
     * @return
     */
    public List<SysMemberOfDefectsLine> memberOfDefectsLine(Long projectId, String timeType);
}
