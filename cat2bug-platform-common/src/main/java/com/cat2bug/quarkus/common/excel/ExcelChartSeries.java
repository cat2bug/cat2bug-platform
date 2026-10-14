package com.cat2bug.quarkus.common.excel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图表数据序列。分类、数值既可引用单元格区域（{@code *Ref} 非空），也可只用字面量；
 * 无论哪种都必须给出缓存值，查看器（预览/移动端）只读缓存不重算。
 *
 * @param name          图例名
 * @param categoryRef   分类区域，如 {@code 'Sheet'!$B$2:$F$2}；为 null 时分类写字面量
 * @param categories    分类文本
 * @param valueRef      数值区域；为 null 时数值写字面量
 * @param values        数值（null 视为空点）
 */
public record ExcelChartSeries(String name, String categoryRef, List<String> categories,
                               String valueRef, List<? extends Number> values) {

    public ExcelChartSeries {
        categories = List.copyOf(categories);
        values = values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    /** 生成 {@code 'Sheet'!$B$2:$F$2} 形式的绝对区域引用（行列均从 0 开始）。 */
    public static String ref(String sheetName, int firstRow, int firstCol, int lastRow, int lastCol) {
        return "'" + sheetName.replace("'", "''") + "'!"
                + absCell(firstRow, firstCol) + ":" + absCell(lastRow, lastCol);
    }

    private static String absCell(int row, int col) {
        return "$" + columnName(col) + "$" + (row + 1);
    }

    static String columnName(int columnIndex) {
        StringBuilder name = new StringBuilder();
        int index = columnIndex + 1;
        while (index > 0) {
            int rem = (index - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            index = (index - 1) / 26;
        }
        return name.toString();
    }
}
