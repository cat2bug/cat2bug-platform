package com.cat2bug.web.service.excel;
import com.cat2bug.web.excel.ExcelTableModel;
import com.cat2bug.web.excel.ExcelTableWriter;
import java.util.List;
import java.util.function.Function;/**
 * 系统管理类固定列 Excel 导出（B9：委托 {@link ExcelTableWriter}）。
 */
final class SimpleExcelExportSupport {record ColumnDef<T>(String header, Function<T, String> formatter) {
    }private SimpleExcelExportSupport() {
    }

    static <T> byte[] export(ExcelTableWriter writer, String sheetName, List<ColumnDef<T>> columns,
                               List<T> items, String errorMessage) {
        List<List<String>> rows = items.stream()
                .map(item -> columns.stream()
                        .map(column -> {
                            String value = column.formatter().apply(item);
                            return value == null ? "" : value;
                        })
                        .toList())
                .toList();
        List<String> headers = columns.stream().map(ColumnDef::header).toList();
        return writer.write(ExcelTableModel.simple(sheetName, headers, rows), errorMessage);
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
