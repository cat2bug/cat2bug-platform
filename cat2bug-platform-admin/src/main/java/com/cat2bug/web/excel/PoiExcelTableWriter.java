package com.cat2bug.web.excel;
import org.springframework.stereotype.Service;
import com.cat2bug.common.exception.ServiceException;
import org.springframework.context.annotation.Profile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;@Profile("!native")
@Service
public class PoiExcelTableWriter implements ExcelTableWriter {@Override
    public byte[] write(ExcelTableModel model, String errorMessage) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(model.sheetName());
            writeHeaders(workbook, sheet, model.headers());
            for (int rowIndex = 0; rowIndex < model.rows().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> values = model.rows().get(rowIndex);
                for (int colIndex = 0; colIndex < values.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    String value = values.get(colIndex);
                    cell.setCellValue(value == null ? "" : value);
                    if (model.wrapTextColumns().contains(colIndex)) {
                        CellStyle wrapStyle = workbook.createCellStyle();
                        wrapStyle.setWrapText(true);
                        cell.setCellStyle(wrapStyle);
                    }
                }
            }
            applyColumnWidths(sheet, model);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException(errorMessage);
        }
    }

    private static void writeHeaders(Workbook workbook, Sheet sheet, List<ExcelHeaderCell> headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            ExcelHeaderCell header = headers.get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(header.text());
            cell.setCellStyle(createHeaderStyle(workbook, header.style()));
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook, ExcelHeaderStyle style) {
        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        if (style == ExcelHeaderStyle.DARK_BAR) {
            font.setColor(IndexedColors.WHITE.getIndex());
            cellStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        } else {
            cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            if (style == ExcelHeaderStyle.REQUIRED) {
                font.setColor(IndexedColors.RED.getIndex());
            }
        }
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFont(font);
        return cellStyle;
    }

    private static void applyColumnWidths(Sheet sheet, ExcelTableModel model) {
        int columnCount = model.headers().size();
        for (int i = 0; i < columnCount; i++) {
            int widthChars = model.columnWidths().getOrDefault(i, model.defaultColumnWidthChars());
            sheet.setColumnWidth(i, widthChars * 256);
        }
    }
}
