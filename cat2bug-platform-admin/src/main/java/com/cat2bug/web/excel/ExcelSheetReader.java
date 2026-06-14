package com.cat2bug.web.excel;
import java.io.IOException;
import java.io.InputStream;/**
 * Excel 首个工作表读取（JVM=POI，Native=FastExcel Reader）。
 */
public interface ExcelSheetReader {ExcelSheetTable readFirstSheet(InputStream input) throws IOException;
}
