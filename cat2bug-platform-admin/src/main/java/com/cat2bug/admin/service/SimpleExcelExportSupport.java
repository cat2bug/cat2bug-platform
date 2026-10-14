package com.cat2bug.admin.service;

import com.cat2bug.common.excel.ExcelTableModel;
import com.cat2bug.common.excel.FastExcelSupport;
import org.dhatim.fastexcel.Worksheet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;

/**
 * 系统管理类固定列 Excel 导出（FastExcel 直写，不依赖 Spring ExcelUtil）。
 */
final class SimpleExcelExportSupport {

    /** 原 POI 列宽 5000（单位 1/256 字符）。 */
    static final double DEFAULT_COLUMN_WIDTH = 5000 / 256.0;
    /** 原 POI 列宽 10000（单位 1/256 字符）。 */
    static final double WIDE_COLUMN_WIDTH = 10000 / 256.0;

    record ColumnDef<T>(String header, Function<T, String> formatter) {
    }

    private SimpleExcelExportSupport() {
    }

    static <T> byte[] export(String sheetName, List<ColumnDef<T>> columns, List<T> rows, String errorMessage) {
        List<String> headers = columns.stream().map(ColumnDef::header).toList();
        List<List<String>> values = new ArrayList<>(rows.size());
        for (T item : rows) {
            List<String> line = new ArrayList<>(columns.size());
            for (ColumnDef<T> column : columns) {
                String value = column.formatter().apply(item);
                line.add(value == null ? "" : value);
            }
            values.add(line);
        }
        return write(ExcelTableModel.simple(sheetName, headers, values), col -> DEFAULT_COLUMN_WIDTH, errorMessage);
    }

    /** 单 Sheet 写出，列宽按 {@code columnWidth}（字符数，可含小数）覆盖模型默认列宽。 */
    static byte[] write(ExcelTableModel model, IntToDoubleFunction columnWidth, String errorMessage) {
        return FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(model.sheetName());
            FastExcelSupport.writeTable(sheet, model, 0);
            for (int i = 0; i < model.headers().size(); i++) {
                sheet.width(i, columnWidth.applyAsDouble(i));
            }
        }, errorMessage);
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String statusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "0" -> "正常";
            case "1" -> "停用";
            default -> status;
        };
    }

    static String dataScopeLabel(String dataScope) {
        if (dataScope == null) {
            return "";
        }
        return switch (dataScope) {
            case "1" -> "所有数据权限";
            case "2" -> "自定义数据权限";
            case "3" -> "本部门数据权限";
            case "4" -> "本部门及以下数据权限";
            case "5" -> "仅本人数据权限";
            default -> dataScope;
        };
    }

    static String yesNoLabel(String value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case "Y" -> "是";
            case "N" -> "否";
            default -> value;
        };
    }
}
