package com.cat2bug.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 出站分包的纯函数：编号、体积闸、重试次数。 */
public final class CloudPushSupport {

    public static final ObjectMapper JSON = new ObjectMapper();

    public static final String TYPE_CORE = "core";
    public static final String TYPE_DOC = "doc";
    public static final String TYPE_GIT = "git";
    public static final String PART_CORE = "core";

    public static final String PART_PENDING = "PENDING";
    public static final String PART_PACKING = "PACKING";
    public static final String PART_UPLOADING = "UPLOADING";
    public static final String PART_OK = "OK";
    public static final String PART_FAILED = "FAILED";

    /** 首次上传之外，自动退避重试次数。 */
    public static final int MAX_AUTO_RETRIES = 3;
    public static final int MAX_UPLOAD_ATTEMPTS = 1 + MAX_AUTO_RETRIES;

    public static final String SIZE_LIMIT_MARK = "超过同步上限";

    /**
     * 预估 zip 时 JSON/文本的保留比例（偏保守，略高于实测压缩比，避免超限误放行）。
     * 附件与 Git bare 接近已压缩，按原体积计。
     */
    public static final double ZIP_JSON_RATIO = 0.18;
    public static final double ZIP_FILE_RATIO = 1.0;

    private CloudPushSupport() {
    }

    /**
     * 把未压缩的 JSON 字节与本地附件字节估成 zip 体积。
     * 文本可压；图片/附件/已压缩对象几乎不缩。
     */
    public static long estimateZipBytes(long jsonBytes, long fileBytes) {
        long json = Math.max(0L, jsonBytes);
        long files = Math.max(0L, fileBytes);
        long packed = Math.round(json * ZIP_JSON_RATIO + files * ZIP_FILE_RATIO);
        if (json == 0L && files == 0L) {
            return 0L;
        }
        // zip 中央目录等固定开销下限，避免空业务包显示 0 却与实包差太多
        return Math.max(packed, 64L);
    }

    public static String docPartId(long docId) {
        return "doc-" + docId;
    }

    public static String gitPartId(String slug) {
        return "git-" + slug;
    }

    /** 两个列表都未传（null）才是旧客户端全量可选内容。 */
    public static boolean isLegacySelection(Object gitSlugs, Object documentIds) {
        return gitSlugs == null && documentIds == null;
    }

    public static boolean shouldUpload(String state, boolean onlyFailed) {
        if (state == null || PART_OK.equals(state)) {
            return false;
        }
        if (onlyFailed) {
            return PART_FAILED.equals(state);
        }
        return true;
    }

    public static boolean wouldExceed(long already, long incoming, long max) {
        if (max < 0) {
            return true;
        }
        if (incoming < 0) {
            incoming = 0;
        }
        if (already < 0) {
            already = 0;
        }
        if (already > max) {
            return true;
        }
        return incoming > (max - already);
    }

    /** {@code failedAttempt} 从 1 起：第 1 次失败等 1s，其后翻倍。 */
    public static long backoffMillis(int failedAttempt) {
        int shift = Math.min(Math.max(failedAttempt, 1) - 1, 5);
        return 1000L << shift;
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("无法计算 SHA-256", e);
        }
        byte[] buffer = new byte[8192];
        try (InputStream in = Files.newInputStream(file)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }
}
