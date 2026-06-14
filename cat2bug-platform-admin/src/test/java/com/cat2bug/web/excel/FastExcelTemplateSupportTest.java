package com.cat2bug.web.excel;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Sheet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastExcelTemplateSupportTest {

    @Test
    void writesTemplateWithHiddenListSheet() throws Exception {
        byte[] bytes = FastExcelTemplateSupport.writeWorkbook("Test", "缺陷数据", ctx -> {
            FastExcelTemplateSupport.writeHeaders(ctx.dataSheet(), List.of(
                    new ExcelHeaderCell("类型", ExcelHeaderStyle.NORMAL),
                    new ExcelHeaderCell("缺陷名称", ExcelHeaderStyle.REQUIRED)
            ));
            var listSheet = FastExcelTemplateSupport.newHiddenListSheet(ctx.workbook(), "_lists");
            var listRange = FastExcelTemplateSupport.writeListColumn(listSheet, 0, List.of("BUG", "需求"));
            FastExcelTemplateSupport.applyListValidation(ctx.dataSheet(), 0, listRange, 1, 10);
            ctx.dataSheet().width(0, 20);
            ctx.dataSheet().width(1, 30);
        });

        assertTrue(bytes.length > 0);
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(bytes))) {
            List<Sheet> sheets = workbook.getSheets().toList();
            assertTrue(sheets.size() >= 2);
            assertEquals("缺陷数据", sheets.get(0).getName());
            assertEquals("_lists", sheets.get(1).getName());
        }
    }
}
