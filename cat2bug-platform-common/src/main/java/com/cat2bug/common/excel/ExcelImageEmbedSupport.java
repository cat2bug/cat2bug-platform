package com.cat2bug.common.excel;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.util.StringUtils;
import org.dhatim.fastexcel.PictureAnchor;
import org.dhatim.fastexcel.Worksheet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 将 {@code /profile/...} 或 http(s) 图片嵌入 FastExcel 单元格（对齐旧 POI {@code IMAGE_LIST}）。
 * FastExcel 仅支持 PNG / JPEG / GIF / SVG；其它格式尽量转 PNG，仍失败则跳过（导出回退写路径文本）。
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
            byte[] embeddable = toEmbeddableImageBytes(data);
            if (embeddable == null || embeddable.length == 0) {
                continue;
            }
            int colOffPx = i * THUMB_PX;
            PictureAnchor anchor = PictureAnchor.oneCellAnchor(row, col, colOffPx, 0, THUMB_PX, THUMB_PX);
            try {
                sheet.addImage(anchor, embeddable);
                embedded++;
            } catch (IllegalArgumentException ignored) {
                /* FastExcel ImageType.fromBytes：非 PNG/JPEG/GIF/SVG 会抛，不得拖垮整次导出 */
            }
        }
        if (embedded == 0) {
            return false;
        }
        sheet.rowHeight(row, IMAGE_ROW_HEIGHT_POINTS);
        return true;
    }

    /**
     * FastExcel 可直接嵌入则原样返回；否则尝试 ImageIO 转 PNG；仍不可用返回 {@code null}。
     */
    public static byte[] toEmbeddableImageBytes(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        if (isFastexcelNativeFormat(data)) {
            return data;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) {
                return null;
            }
            byte[] png = out.toByteArray();
            return png.length > 0 ? png : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 与 {@code org.dhatim.fastexcel.ImageType#fromBytes} 可识别的魔数对齐。 */
    public static boolean isFastexcelNativeFormat(byte[] data) {
        if (data == null || data.length < 3) {
            return false;
        }
        // PNG
        if (data.length >= 8
                && (data[0] & 0xFF) == 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47) {
            return true;
        }
        // JPEG
        if ((data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return true;
        }
        // GIF
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            return true;
        }
        // SVG（文本）
        int probeLen = Math.min(data.length, 512);
        String head = new String(data, 0, probeLen, StandardCharsets.UTF_8).trim().toLowerCase();
        return head.contains("<svg") || (head.startsWith("<?xml") && head.contains("<svg"));
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
