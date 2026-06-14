package com.cat2bug.web.service.excel;/**
 * 缺陷 / 成员走势 Excel 导出（B9：JVM=POI 含图表，Native=FastExcel 仅数据表）。
 */
public interface DefectTrendExcelExportService {byte[] exportDefectStateLine(Long projectId, String timeType);byte[] exportMemberDefectLine(Long projectId, String timeType);
}
