package com.cat2bug.web.excel;
import org.springframework.stereotype.Service;
import org.dhatim.fastexcel.Range;
import org.dhatim.fastexcel.VisibilityState;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;/**
 * FastExcel 导入模版：表头、隐藏列表页与下拉校验（B9 Phase 2）。
 */
public final class FastExcelTemplateSupport {private static final String LIGHT_HEADER_FILL = "C0C0C0";
    private static final String REQUIRED_FONT = "FF0000";private FastExcelTemplateSupport() {
    }

    public static byte[] writeWorkbook(String producer, String sheetName,
                                       Consumer<TemplateWorkbookContext> builder) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook workbook = new Workbook(out, producer, "1.0");
            TemplateWorkbookContext context = new TemplateWorkbookContext(workbook, workbook.newWorksheet(sheetName));
            builder.accept(context);
            workbook.finish();
            return out.toByteArray();
        }
    }

    public static Worksheet newHiddenListSheet(Workbook workbook, String listSheetName) {
        Worksheet listSheet = workbook.newWorksheet(listSheetName);
        listSheet.setVisibilityState(VisibilityState.VERY_HIDDEN);
        return listSheet;
    }

    public static void writeHeaders(Worksheet sheet, List<ExcelHeaderCell> headers) {
        for (int i = 0; i < headers.size(); i++) {
            ExcelHeaderCell header = headers.get(i);
            sheet.value(0, i, header.text());
            var style = sheet.style(0, i).bold();
            if (header.style() == ExcelHeaderStyle.DARK_BAR) {
                style.fillColor("808080").fontColor("FFFFFF");
            } else {
                style.fillColor(LIGHT_HEADER_FILL);
                if (header.style() == ExcelHeaderStyle.REQUIRED) {
                    style.fontColor(REQUIRED_FONT);
                }
            }
            style.set();
        }
    }/**
     * 在隐藏表写入一列列表值，返回可供 {@link #applyListValidation} 引用的 Range。
     */
    public static Range writeListColumn(Worksheet listSheet, int columnIndex, List<String> values) {
        List<String> safeValues = values == null || values.isEmpty() ? List.of("") : values;
        for (int row = 0; row < safeValues.size(); row++) {
            listSheet.value(row, columnIndex, safeValues.get(row));
        }
        return listSheet.range(0, columnIndex, safeValues.size() - 1, columnIndex);
    }/**
     * 对数据区一列应用下拉（行号 0-based，含起止行）。
     */
    public static void applyListValidation(Worksheet dataSheet, int columnIndex, Range listRange,
                                           int firstDataRow, int lastDataRow) {
        if (columnIndex < 0 || listRange == null) {
            return;
        }
        Range target = dataSheet.range(firstDataRow, columnIndex, lastDataRow, columnIndex);
        target.validateWithList(listRange).showDropdown(true).allowBlank(true).showErrorMessage(true);
    }

    public static void applyColumnWidths(Worksheet sheet, Map<Integer, Integer> columnWidths, int defaultWidth) {
        int maxColumn = columnWidths.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        for (int i = 0; i <= maxColumn; i++) {
            sheet.width(i, columnWidths.getOrDefault(i, defaultWidth));
        }
    }

    public static final class TemplateWorkbookContext {
        private final Workbook workbook;
        private final Worksheet dataSheet;TemplateWorkbookContext(Workbook workbook, Worksheet dataSheet) {
            this.workbook = workbook;
            this.dataSheet = dataSheet;
        }

    public Workbook workbook() {
            return workbook;
        }

    public Worksheet dataSheet() {
            return dataSheet;
        }
    }
}
