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
        String exact = TeamModuleLabelContext.exact(code);
        String pattern = exact != null
                ? exact
                : TeamModuleLabelContext.apply(resolve(code, MessageLocaleContext.get()));
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }

    private static String resolve(String code, Locale locale) {
        if (code == null) {
            return "";
        }
        Locale effective = locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
        String found = lookup(code, effective);
        if (found != null) {
            return found;
        }
        if (!Locale.SIMPLIFIED_CHINESE.equals(effective)) {
            found = lookup(code, Locale.SIMPLIFIED_CHINESE);
            if (found != null) {
                return found;
            }
        }
        return code;
    }

    private static String lookup(String code, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale);
            if (bundle.containsKey(code)) {
                return bundle.getString(code);
            }
        } catch (MissingResourceException ignored) {
            // fall through
        }
        return null;
    }
}
