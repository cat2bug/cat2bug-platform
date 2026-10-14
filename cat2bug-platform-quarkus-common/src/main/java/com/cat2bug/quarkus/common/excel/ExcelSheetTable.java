package com.cat2bug.quarkus.common.excel;

import com.cat2bug.quarkus.common.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 首个工作表的行列文本快照（行、列均从 0 开始；缺失单元格为空串）。
 */
public record ExcelSheetTable(List<List<String>> rows) {

    public ExcelSheetTable {
        rows = rows != null ? rows : List.of();
    }

    public int rowCount() {
        return rows.size();
    }

    public int columnCount(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return 0;
        }
        return rows.get(rowIndex).size();
    }

    public String cell(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        List<String> row = rows.get(rowIndex);
        if (columnIndex < 0 || columnIndex >= row.size()) {
            return null;
        }
        return row.get(columnIndex);
    }

    public String cellText(int rowIndex, int columnIndex) {
        String value = cell(rowIndex, columnIndex);
        return value == null ? "" : value.trim();
    }

    /** 前 5 行中第一个非空行视为表头。 */
    public int findHeaderRowIndex() {
        int limit = Math.min(5, rowCount());
        for (int r = 0; r < limit; r++) {
            for (String cell : rows.get(r)) {
                if (StringUtils.isNotEmpty(cell)) {
                    return r;
                }
            }
        }
        return 0;
    }

    public List<String> headerRow(int headerRowIndex) {
        if (headerRowIndex < 0 || headerRowIndex >= rowCount()) {
            return List.of();
        }
        return new ArrayList<>(rows.get(headerRowIndex));
    }

    public boolean isRowEmpty(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount()) {
            return true;
        }
        for (String cell : rows.get(rowIndex)) {
            if (StringUtils.isNotBlank(cell)) {
                return false;
            }
        }
        return true;
    }
}
