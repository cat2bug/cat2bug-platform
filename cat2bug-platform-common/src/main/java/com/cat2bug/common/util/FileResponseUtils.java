package com.cat2bug.common.util;

import com.cat2bug.common.web.BinaryHttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 对齐 Spring FileUtils 中附件下载相关方法的最小子集。 */
public final class FileResponseUtils {

    private FileResponseUtils() {
    }

    public static void setAttachmentResponseHeader(BinaryHttpResponse response, String realFileName)
            throws IOException {
        String percentEncoded = URLEncoder.encode(realFileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + percentEncoded + ";filename*=utf-8''" + percentEncoded);
        response.setHeader("download-filename", percentEncoded);
    }

    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        Files.copy(Path.of(filePath), os);
        os.flush();
    }

    public static boolean deleteFile(String filePath) {
        try {
            return Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            return false;
        }
    }
}
