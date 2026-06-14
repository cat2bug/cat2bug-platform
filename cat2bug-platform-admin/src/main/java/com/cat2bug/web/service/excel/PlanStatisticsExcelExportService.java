package com.cat2bug.web.service.excel;/**
 * 测试计划统计 Excel 导出（B9：JVM=POI 含图表，Native=FastExcel 仅数据表）。
 */
public interface PlanStatisticsExcelExportService {byte[] exportPlanStatistics(Long projectId);
}
