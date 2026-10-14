package com.cat2bug.common.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 对齐 Spring MessageUtils：从 classpath {@code i18n/messages} 取文案。
 * 找不到键时回退为键本身。
 */
public final class MessageUtils {

    private static final String BUNDLE = "i18n.messages";

    private MessageUtils() {
    }

    public static String message(String code, Object... args) {
        String pattern = resolve(code, Locale.SIMPLIFIED_CHINESE);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }

    private static String resolve(String code, Locale locale) {
        if (code == null) {
            return "";
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale);
            if (bundle.containsKey(code)) {
                return bundle.getString(code);
            }
        } catch (MissingResourceException ignored) {
            // fall through
        }
        return code;
    }
}
