package com.cat2bug.web.excel;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;/**
 * 通用二维表 Excel 模型（单 Sheet）。
 */
public record ExcelTableModel(
        String sheetName,
        List<ExcelHeaderCell> headers,
        List<List<String>> rows,
        int defaultColumnWidthChars,
        Map<Integer, Integer> columnWidths,
        Set<Integer> wrapTextColumns) {public ExcelTableModel {
        rows = rows != null ? rows : List.of();
        columnWidths = columnWidths != null ? columnWidths : Map.of();
        wrapTextColumns = wrapTextColumns != null ? wrapTextColumns : Set.of();
    }

    public static ExcelTableModel simple(String sheetName, List<String> headerTexts, List<List<String>> rows) {
        List<ExcelHeaderCell> headers = headerTexts.stream()
                .map(text -> new ExcelHeaderCell(text, ExcelHeaderStyle.DARK_BAR))
                .toList();
        return new ExcelTableModel(sheetName, headers, rows, 20, Map.of(), Set.of());
    }

    public static ExcelTableModel withHeaders(String sheetName, List<ExcelHeaderCell> headers,
                                              List<List<String>> rows, int defaultWidth) {
        return new ExcelTableModel(sheetName, headers, rows, defaultWidth, Map.of(), Set.of());
    }

    public ExcelTableModel withColumnWidth(int columnIndex, int widthChars) {
        return new ExcelTableModel(sheetName, headers, rows, defaultColumnWidthChars,
                Map.of(columnIndex, widthChars), wrapTextColumns);
    }

    public ExcelTableModel withWrapTextColumns(Set<Integer> columns) {
        return new ExcelTableModel(sheetName, headers, rows, defaultColumnWidthChars, columnWidths, columns);
    }

    public static ExcelTableModel emptyHeaders(String sheetName, List<ExcelHeaderCell> headers, int defaultWidth) {
        return new ExcelTableModel(sheetName, headers, Collections.emptyList(), defaultWidth, Map.of(), Set.of());
    }
}
