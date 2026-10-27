package com.cat2bug.admin;

import com.cat2bug.common.excel.ExcelDrawingImageReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ExcelDrawingImageReaderTest {

    @Test
    void readsFloatingImageAnchoredToCustomImageColumn() throws Exception {
        Path sample = Path.of(System.getProperty("user.home"), "Downloads", "缺陷模版1793104707394.xlsx");
        assumeTrue(Files.isRegularFile(sample), "本地样例模版不存在时跳过");

        byte[] bytes = Files.readAllBytes(sample);
        Map<String, List<ExcelDrawingImageReader.EmbeddedImage>> byCell =
                ExcelDrawingImageReader.readByCell(bytes);

        assertFalse(byCell.isEmpty(), "应解析到悬浮图");
        // drawing from: row=1 col=7 → 数据行「自定义图片」列
        List<ExcelDrawingImageReader.EmbeddedImage> images = byCell.get("1_7");
        assertTrue(images != null && !images.isEmpty());
        assertEquals("image1.png", images.get(0).fileName());
        assertTrue(images.get(0).data().length > 100);
    }
}
