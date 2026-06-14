package com.cat2bug.web.excel;
import org.springframework.stereotype.Service;
import com.cat2bug.common.exception.ServiceException;
import org.springframework.context.annotation.Profile;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;@Profile("native")
@Service
public class FastExcelTableWriter implements ExcelTableWriter {private static final String DARK_BAR_FILL = "808080";
    private static final String LIGHT_BAR_FILL = "C0C0C0";
    private static final String REQUIRED_FONT = "FF0000";
    private static final String DARK_BAR_FONT = "FFFFFF";@Override
    public byte[] write(ExcelTableModel model, String errorMessage) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Workbook workbook = new Workbook(out, "Cat2Bug", "1.0");
            Worksheet sheet = workbook.newWorksheet(model.sheetName());
            writeHeaders(sheet, model.headers());
            for (int rowIndex = 0; rowIndex < model.rows().size(); rowIndex++) {
                List<String> values = model.rows().get(rowIndex);
                for (int colIndex = 0; colIndex < values.size(); colIndex++) {
                    String value = values.get(colIndex);
                    if (value != null && !value.isEmpty()) {
                        sheet.value(rowIndex + 1, colIndex, value);
                        if (model.wrapTextColumns().contains(colIndex)) {
                            sheet.style(rowIndex + 1, colIndex).wrapText(true).set();
                        }
                    }
                }
            }
            applyColumnWidths(sheet, model);
            workbook.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceException(errorMessage);
        }
    }

    private static void writeHeaders(Worksheet sheet, List<ExcelHeaderCell> headers) {
        for (int i = 0; i < headers.size(); i++) {
            ExcelHeaderCell header = headers.get(i);
            sheet.value(0, i, header.text());
            var style = sheet.style(0, i).bold();
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

    private static void applyColumnWidths(Worksheet sheet, ExcelTableModel model) {
        for (int i = 0; i < model.headers().size(); i++) {
            sheet.width(i, model.columnWidths().getOrDefault(i, model.defaultColumnWidthChars()));
        }
    }
}
