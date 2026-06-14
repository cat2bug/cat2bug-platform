package com.cat2bug.web.excel;/**
 * Excel 表格式写入（JVM=POI，Native=FastExcel）。
 */
public interface ExcelTableWriter {byte[] write(ExcelTableModel model, String errorMessage);
}
