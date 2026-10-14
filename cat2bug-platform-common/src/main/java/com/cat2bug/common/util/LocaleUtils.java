package com.cat2bug.common.util;

import java.util.Locale;

/**
 * 解析前端 language 请求头（如 zh_CN、en_US）为 Java Locale。
 */
public final class LocaleUtils {

    public static final Locale[] MESSAGE_BUNDLE_LOCALES = {
            Locale.SIMPLIFIED_CHINESE,
            Locale.TRADITIONAL_CHINESE,
            Locale.US,
            new Locale("ar"),
            Locale.JAPAN,
            Locale.KOREA,
            new Locale("ru")
    };

    private LocaleUtils() {
    }

    public static Locale parseLanguageHeader(String language) {
        if (StringUtils.isEmpty(language)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        String normalized = language.trim().replace('-', '_');
        String[] parts = normalized.split("_");
        if (parts.length >= 2 && StringUtils.isNotEmpty(parts[0]) && StringUtils.isNotEmpty(parts[1])) {
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(parts[0]);
    }
}
