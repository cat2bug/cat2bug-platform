package com.cat2bug.admin;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.excel.ExcelImageEmbedSupport;
import com.cat2bug.common.excel.FastExcelSupport;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelImageEmbedSupportTest {

    @Test
    void embedsLocalProfileImageIntoWorksheet() throws Exception {
        Path profile = Files.createTempDirectory("cat2bug-excel-profile-");
        Path image = profile.resolve("upload").resolve("t.png");
        Files.createDirectories(image.getParent());
        Files.write(image, minimalPng());
        Cat2BugConfig.configure(profile.toString(), "git", 120);

        byte[] bytes = FastExcelSupport.writeWorkbook(workbook -> {
            Worksheet sheet = workbook.newWorksheet("t");
            sheet.value(0, 0, "图片");
            boolean ok = ExcelImageEmbedSupport.embedInCell(
                    sheet, 1, 0, "/profile/upload/t.png", null);
            assertTrue(ok);
        }, "嵌图失败");

        assertTrue(bytes.length > 0);
        try (XSSFWorkbook poi = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertNotNull(poi.getSheetAt(0).getDrawingPatriarch());
            assertFalse(poi.getAllPictures().isEmpty());
        }
    }

    @Test
    void loadBytesReturnsNullForMissingFile() {
        Cat2BugConfig.configure("./upload-missing-" + System.nanoTime(), "git", 120);
        assertTrue(ExcelImageEmbedSupport.loadBytes("/profile/upload/no-such.png", null) == null);
    }

    private static byte[] minimalPng() {
        // 1x1 RGB PNG
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
                (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
                0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF,
                (byte) 0xC0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x01, 0x00,
                0x05, (byte) 0xFE, (byte) 0xD4, (byte) 0xEF, 0x00, 0x00,
                0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42,
                0x60, (byte) 0x82
        };
    }
}
