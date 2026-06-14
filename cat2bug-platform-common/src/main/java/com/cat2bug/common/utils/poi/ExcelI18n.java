package com.cat2bug.common.utils.poi;

import com.cat2bug.common.utils.LocaleUtils;
import com.cat2bug.common.utils.StringUtils;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel 表头 i18n（与 Spring MessageUtils.message 键一致）。
 */
public final class ExcelI18n {

    private static final String BUNDLE_BASE = "i18n/messages";
    private static final ConcurrentHashMap<Locale, ResourceBundle> CACHE = new ConcurrentHashMap<>();

    private ExcelI18n() {
    }

    public static String message(String key, Locale locale) {
        if (StringUtils.isEmpty(key)) {
            return "";
        }
        Locale effective = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        try {
            return bundle(effective).getString(key);
        } catch (MissingResourceException ignored) {
            if (!Locale.SIMPLIFIED_CHINESE.equals(effective)) {
                try {
                    return bundle(Locale.SIMPLIFIED_CHINESE).getString(key);
                } catch (MissingResourceException ignoredAgain) {
                    return "";
                }
            }
            return "";
        }
    }

    public static String header(String i18nKey, String defaultHeader, Locale locale) {
        String translated = message(i18nKey, locale);
        return StringUtils.isNotEmpty(translated) ? translated : defaultHeader;
    }

    public static String requiredHeader(String i18nKey, String requiredI18nKey, String defaultHeader, Locale locale) {
        String required = message(requiredI18nKey, locale);
        if (StringUtils.isNotEmpty(required)) {
            return required;
        }
        String base = header(i18nKey, defaultHeader, locale);
        if (base.contains("(必填)") || base.contains("(required)")) {
            return base;
        }
        return base + "(必填)";
    }

    public static java.util.Set<String> allTranslations(String key) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        for (Locale locale : LocaleUtils.MESSAGE_BUNDLE_LOCALES) {
            String text = message(key, locale);
            if (StringUtils.isNotEmpty(text)) {
                set.add(text.trim());
            }
        }
        return set;
    }

    private static ResourceBundle bundle(Locale locale) {
        return CACHE.computeIfAbsent(locale, loc -> ResourceBundle.getBundle(BUNDLE_BASE, loc,
                Thread.currentThread().getContextClassLoader()));
    }
}
