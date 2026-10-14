package com.cat2bug.quarkus.common.excel;

import com.cat2bug.quarkus.common.exception.ApiException;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * FastExcel 读写（xlsx）。不依赖 POI/xmlbeans，JVM 与 Native 行为一致。
 * <p>
 * 简单二维表用 {@link #write(ExcelTableModel, String)}；需要多 Sheet 或自定义样式时用
 * {@link #writeWorkbook(WorkbookWriter, String)} 直接操作 {@link Workbook}。
 */
public final class FastExcelSupport {

    public static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String DARK_BAR_FILL = "808080";
    public static final String LIGHT_BAR_FILL = "C0C0C0";
    public static final String DARK_BAR_FONT = "FFFFFF";
    public static final String REQUIRED_FONT = "FF0000";

    private static final String APPLICATION_NAME = "Cat2Bug";
    private static final String APPLICATION_VERSION = "1.0";

    @FunctionalInterface
    public interface WorkbookWriter {
        void write(Workbook workbook) throws IOException;
    }

    private FastExcelSupport() {
    }

    public static byte[] write(ExcelTableModel model, String errorMessage) {
        return writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet(model.sheetName());
            writeTable(sheet, model, 0);
        }, errorMessage);
    }

    public static byte[] writeWorkbook(WorkbookWriter writer, String errorMessage) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook workbook = new Workbook(out, APPLICATION_NAME, APPLICATION_VERSION);
            writer.write(workbook);
            workbook.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(errorMessage);
        }
    }

    /**
     * 从 {@code startRow} 起写入表头 + 数据并设置列宽。
     *
     * @return 写入后下一个空行号
     */
    public static int writeTable(Worksheet sheet, ExcelTableModel model, int startRow) {
        writeHeaders(sheet, model.headers(), startRow);
        int rowIndex = startRow + 1;
        for (List<String> values : model.rows()) {
            for (int col = 0; col < values.size(); col++) {
                String value = values.get(col);
                if (value != null && !value.isEmpty()) {
                    sheet.value(rowIndex, col, value);
                    if (model.wrapTextColumns().contains(col)) {
                        sheet.style(rowIndex, col).wrapText(true).set();
                    }
                }
            }
            rowIndex++;
        }
        int columns = model.headers().size();
        for (int col = 0; col < columns; col++) {
            sheet.width(col, model.columnWidths().getOrDefault(col, model.defaultColumnWidthChars()));
        }
        return rowIndex;
    }

    public static void writeHeaders(Worksheet sheet, List<ExcelHeaderCell> headers, int row) {
        for (int col = 0; col < headers.size(); col++) {
            ExcelHeaderCell header = headers.get(col);
            sheet.value(row, col, header.text());
            var style = sheet.style(row, col).bold();
            if (header.style() == ExcelHeaderStyle.DARK_BAR) {
                style.fillColor(DARK_BAR_FILL).fontColor(DARK_BAR_FONT);
            } else {
                style.fillColor(LIGHT_BAR_FILL);
                if (header.style() == ExcelHeaderStyle.REQUIRED) {
                    style.fontColor(REQUIRED_FONT);
                }
            }
            style.set();
        }
    }

    /** 读取首个工作表为文本矩阵（数字/日期按 FastExcel 的原始文本返回）。 */
    public static ExcelSheetTable readFirstSheet(InputStream input) throws IOException {
        try (ReadableWorkbook workbook = new ReadableWorkbook(input)) {
            Sheet sheet = workbook.getFirstSheet();
            List<List<String>> rows = new ArrayList<>();
            try (Stream<Row> stream = sheet.openStream()) {
                stream.forEach(row -> {
                    int rowNum = row.getRowNum() - 1;
                    while (rows.size() < rowNum) {
                        rows.add(new ArrayList<>());
                    }
                    List<String> cells = new ArrayList<>();
                    row.forEach(cell -> {
                        if (cell == null) {
                            return;
                        }
                        int columnIndex = cell.getColumnIndex();
                        while (cells.size() <= columnIndex) {
                            cells.add("");
                        }
                        String text = cell.getText();
                        cells.set(columnIndex, text == null ? "" : text);
                    });
                    rows.add(cells);
                });
            }
            return new ExcelSheetTable(rows);
        }
    }
}
