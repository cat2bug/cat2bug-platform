package com.cat2bug.web.excel;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;@Profile("native")
@Service
public class FastExcelSheetReader implements ExcelSheetReader {@Override
    public ExcelSheetTable readFirstSheet(InputStream input) throws IOException {
        try (ReadableWorkbook workbook = new ReadableWorkbook(input)) {
            Sheet sheet = workbook.getFirstSheet();
            List<List<String>> rows = new ArrayList<>();
            try (Stream<Row> stream = sheet.openStream()) {
                stream.forEach(row -> {
                    List<String> cells = new ArrayList<>();
                    row.forEach(cell -> {
                        int columnIndex = cell.getColumnIndex();
                        while (cells.size() <= columnIndex) {
                            cells.add("");
                        }
                        cells.set(columnIndex, cell.getText());
                    });
                    rows.add(cells);
                });
            }
            return new ExcelSheetTable(rows);
        }
    }
}
