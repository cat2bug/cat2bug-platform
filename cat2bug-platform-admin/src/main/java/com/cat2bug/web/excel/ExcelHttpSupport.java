package com.cat2bug.web.excel;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;/**
 * 将 FastExcel/POI SPI 产出的 xlsx 字节写入 HTTP 响应。
 */
public final class ExcelHttpSupport {public static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";private ExcelHttpSupport() {
    }

    public static void write(HttpServletResponse response, byte[] workbook, String filename) throws IOException {
        if (workbook == null || workbook.length == 0) {
            throw new IOException("Excel 内容为空");
        }
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(SPREADSHEET_MEDIA_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(workbook);
        response.flushBuffer();
    }
}
