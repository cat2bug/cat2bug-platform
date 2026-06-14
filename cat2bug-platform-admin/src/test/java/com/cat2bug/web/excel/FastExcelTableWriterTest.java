package com.cat2bug.web.excel;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastExcelTableWriterTest {

    @Test
    void writesHeaderAndDataRows() throws Exception {
        ExcelTableModel model = ExcelTableModel.withHeaders(
                "缺陷数据",
                List.of(
                        new ExcelHeaderCell("缺陷名称", ExcelHeaderStyle.REQUIRED),
                        new ExcelHeaderCell("描述", ExcelHeaderStyle.NORMAL)
                ),
                List.of(List.of("FastExcel 导出测试", "描述")),
                20);

        byte[] bytes = new FastExcelTableWriter().write(model, "导出失败");

        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getFirstSheet();
            try (Stream<Row> rows = sheet.openStream()) {
                List<Row> rowList = rows.limit(2).toList();
                assertEquals(2, rowList.size());
                assertEquals("缺陷名称", rowList.get(0).getCellText(0));
                assertEquals("FastExcel 导出测试", rowList.get(1).getCellText(0));
                assertEquals("描述", rowList.get(1).getCellText(1));
            }
        }
        assertTrue(bytes.length > 0);
    }
}
