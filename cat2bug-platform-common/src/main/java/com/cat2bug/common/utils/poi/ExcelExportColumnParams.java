package com.cat2bug.common.utils.poi;

/**
 * Excel 动态列导出参数名（无 POI / ExcelUtil 依赖，Native 与 JVM 共用）。
 */
public final class ExcelExportColumnParams
{
    public static final String PARAM_EXPORT_COLUMNS = "exportColumns";
    public static final String PARAM_EXPORT_SCOPE = "exportScope";
    public static final String SCOPE_DATA = "data";
    public static final String SCOPE_IMPORT_TEMPLATE = "importTemplate";

    private ExcelExportColumnParams()
    {
    }
}
