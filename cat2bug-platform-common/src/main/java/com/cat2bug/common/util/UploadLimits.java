package com.cat2bug.common.util;

/** 系统上传上限（{@code sys_config}）与字节换算。 */
public final class UploadLimits {

    public static final String CONFIG_KEY = "sys.upload.maxFileSizeMb";
    public static final int DEFAULT_MB = 500;

    private UploadLimits() {
    }

    /** 非法或空配置回退 {@link #DEFAULT_MB}。 */
    public static int parseSystemMb(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MB;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= 1 ? value : DEFAULT_MB;
        } catch (NumberFormatException e) {
            return DEFAULT_MB;
        }
    }

    public static long toBytes(int mb) {
        return mb * 1024L * 1024L;
    }
}
