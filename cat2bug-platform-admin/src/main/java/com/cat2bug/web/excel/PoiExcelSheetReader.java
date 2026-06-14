package com.cat2bug.web.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;@Profile("!native")
@Service
public class PoiExcelSheetReader implements ExcelSheetReader {private final DataFormatter formatter = new DataFormatter();@Override
    public ExcelSheetTable readFirstSheet(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return new ExcelSheetTable(List.of());
            }
            List<List<String>> rows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    rows.add(List.of());
                    continue;
                }
                List<String> cells = new ArrayList<>();
                short lastCell = row.getLastCellNum();
                if (lastCell < 0) {
                    rows.add(List.of());
                    continue;
                }
                for (int colIndex = 0; colIndex < lastCell; colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    cells.add(cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(cells);
            }
            return new ExcelSheetTable(rows);
        }
    }
}
