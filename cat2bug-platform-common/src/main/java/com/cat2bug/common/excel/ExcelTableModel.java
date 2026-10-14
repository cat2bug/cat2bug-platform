package com.cat2bug.common.excel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用二维表 Excel 模型（单 Sheet；第 0 行为表头，数据从第 1 行开始）。
 *
 * @param defaultColumnWidthChars 默认列宽（字符数）
 * @param columnWidths            个别列宽覆盖（列号 → 字符数）
 * @param wrapTextColumns         数据区自动换行的列
 */
public record ExcelTableModel(
        String sheetName,
        List<ExcelHeaderCell> headers,
        List<List<String>> rows,
        int defaultColumnWidthChars,
        Map<Integer, Integer> columnWidths,
        Set<Integer> wrapTextColumns) {

    public ExcelTableModel {
        rows = rows != null ? rows : List.of();
        columnWidths = columnWidths != null ? columnWidths : Map.of();
        wrapTextColumns = wrapTextColumns != null ? wrapTextColumns : Set.of();
    }

    /** 深灰表头 + 默认列宽 20。 */
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
        Map<Integer, Integer> widths = new HashMap<>(columnWidths);
        widths.put(columnIndex, widthChars);
        return new ExcelTableModel(sheetName, headers, rows, defaultColumnWidthChars, widths, wrapTextColumns);
    }

    public ExcelTableModel withWrapTextColumns(Set<Integer> columns) {
        return new ExcelTableModel(sheetName, headers, rows, defaultColumnWidthChars, columnWidths, columns);
    }
}
