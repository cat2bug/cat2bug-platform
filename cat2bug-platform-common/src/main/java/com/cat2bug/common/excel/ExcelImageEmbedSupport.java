package com.cat2bug.common.excel;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.util.StringUtils;
import org.dhatim.fastexcel.PictureAnchor;
import org.dhatim.fastexcel.Worksheet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 {@code /profile/...} 或 http(s) 图片嵌入 FastExcel 单元格（对齐旧 POI {@code IMAGE_LIST}）。
 */
public final class ExcelImageEmbedSupport {

    public static final String RESOURCE_PREFIX = "/profile";
    /** 与旧 {@code @Excel(width = 50, height = 50)} 一致。 */
    public static final double IMAGE_COLUMN_WIDTH_CHARS = 50d;
    public static final double IMAGE_ROW_HEIGHT_POINTS = 50d;
    private static final int THUMB_PX = 64;
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private ExcelImageEmbedSupport() {
    }

    public static List<String> splitPaths(String raw) {
        List<String> paths = new ArrayList<>();
        if (StringUtils.isEmpty(raw)) {
            return paths;
        }
        for (String part : raw.split("[,，]")) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                paths.add(trimmed);
            }
        }
        return paths;
    }

    /**
     * 在单元格内嵌入多张缩略图；全部加载失败时返回 {@code false}（调用方应回退写 URL 文本）。
     *
     * @param assetHost 可选，形如 {@code http://localhost:2222/dev-api}，本地缺文件时拼绝对 URL
     */
    public static boolean embedInCell(Worksheet sheet, int row, int col, String rawPaths, String assetHost) {
        List<String> paths = splitPaths(rawPaths);
        if (paths.isEmpty()) {
            return false;
        }
        int embedded = 0;
        for (int i = 0; i < paths.size(); i++) {
            byte[] data = loadBytes(paths.get(i), assetHost);
            if (data == null || data.length == 0) {
                continue;
            }
            int colOffPx = i * THUMB_PX;
            PictureAnchor anchor = PictureAnchor.oneCellAnchor(row, col, colOffPx, 0, THUMB_PX, THUMB_PX);
            sheet.addImage(anchor, data);
            embedded++;
        }
        if (embedded == 0) {
            return false;
        }
        sheet.rowHeight(row, IMAGE_ROW_HEIGHT_POINTS);
        return true;
    }

    public static byte[] loadBytes(String imagePath, String assetHost) {
        if (StringUtils.isEmpty(imagePath)) {
            return null;
        }
        String path = imagePath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return readHttp(path);
        }
        byte[] local = readLocal(path);
        if (local != null && local.length > 0) {
            return local;
        }
        if (StringUtils.isEmpty(assetHost)) {
            return null;
        }
        String host = assetHost.trim().replaceAll("/+$", "");
        if (host.isEmpty()) {
            return null;
        }
        String url = path.startsWith("/") ? host + path : host + "/" + path;
        return readHttp(url);
    }

    private static byte[] readLocal(String path) {
        try {
            String relative = path.replace('\\', '/');
            int idx = relative.indexOf(RESOURCE_PREFIX);
            if (idx >= 0) {
                relative = relative.substring(idx + RESOURCE_PREFIX.length());
            }
            while (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            if (relative.isBlank() || relative.contains("..")) {
                return null;
            }
            Path root = Paths.get(Cat2BugConfig.getProfile()).toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return null;
            }
            return Files.readAllBytes(file);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static byte[] readHttp(String url) {
        try {
            URLConnection connection = URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoInput(true);
            try (InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
