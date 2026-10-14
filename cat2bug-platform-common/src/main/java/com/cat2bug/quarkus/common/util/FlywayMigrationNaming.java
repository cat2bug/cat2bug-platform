package com.cat2bug.quarkus.common.util;

/**
 * Flyway 脚本命名（与 Spring {@code com.cat2bug.common.util.FlywayMigrationNaming} 规则一致）。
 */
public final class FlywayMigrationNaming {

    public static final String LEGACY_LINE_MAX_VERSION = "0.6.2.18";
    public static final String PRODUCT_VERSION = "1.0.0";

    private FlywayMigrationNaming() {
    }

    public static String versionPrefix(String productVersion) {
        if (StringUtils.isBlank(productVersion)) {
            throw new IllegalArgumentException("productVersion 不能为空");
        }
        String[] parts = productVersion.trim().split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("productVersion 须为 major.minor.patch，例如 1.0.0");
        }
        for (String part : parts) {
            if (!part.matches("\\d+")) {
                throw new IllegalArgumentException("productVersion 各段须为数字: " + productVersion);
            }
        }
        return "V" + parts[0] + "_" + parts[1] + "_" + parts[2];
    }

    public static String serialFileName(String productVersion, int serial, String slug) {
        validateSlug(slug);
        if (serial < 1) {
            throw new IllegalArgumentException("serial 须 >= 1");
        }
        return versionPrefix(productVersion) + "_" + serial + "__" + normalizeSlug(slug) + ".sql";
    }

    public static String baselineFileName(String productVersion, String slug) {
        validateSlug(slug);
        return versionPrefix(productVersion) + "__" + normalizeSlug(slug) + ".sql";
    }

    private static void validateSlug(String slug) {
        if (StringUtils.isBlank(slug)) {
            throw new IllegalArgumentException("slug 不能为空");
        }
        if (!normalizeSlug(slug).matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("slug 仅允许小写字母、数字与下划线");
        }
    }

    private static String normalizeSlug(String slug) {
        return slug.trim().toLowerCase().replace('-', '_');
    }
}
