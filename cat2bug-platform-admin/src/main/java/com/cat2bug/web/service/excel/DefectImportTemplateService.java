package com.cat2bug.web.service.excel;
import java.util.Locale;
import java.util.Map;/**
 * 缺陷 Excel 导入模版（B9：JVM=POI 含下拉，Native=FastExcel 表头）。
 */
public interface DefectImportTemplateService {byte[] buildTemplateWorkbook(Map<String, Object> params, Locale locale);byte[] buildTemplateWorkbook(Long projectId, Map<String, Object> params, Locale locale);
}
