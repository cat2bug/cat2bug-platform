package com.cat2bug.quarkus.common.util;

public final class StringUtils {

    public static final String EMPTY = "";

    private StringUtils() {
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isEmpty(trimmed) ? null : trimmed;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /** 对齐 Apache Commons / Hutool：空白时返回默认值。 */
    public static String defaultIfBlank(String str, String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    public static boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static boolean ishttp(String link) {
        return isNotEmpty(link) && (link.startsWith("http://") || link.startsWith("https://"));
    }

    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static <T> boolean isNotEmpty(java.util.Collection<T> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static <T> boolean isEmpty(java.util.Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static String[] split(String str, String separator) {
        if (isEmpty(str)) {
            return new String[0];
        }
        return str.split(separator);
    }
}
