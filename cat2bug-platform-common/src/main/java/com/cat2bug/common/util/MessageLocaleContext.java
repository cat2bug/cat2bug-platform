package com.cat2bug.common.util;

import java.util.Locale;

/**
 * 请求级语言（来自前端 {@code language} 头），供 {@link MessageUtils} 取文案。
 */
public final class MessageLocaleContext {

    private static final ThreadLocal<Locale> HOLDER = new ThreadLocal<>();

    private MessageLocaleContext() {
    }

    public static void set(Locale locale) {
        if (locale == null) {
            HOLDER.remove();
        } else {
            HOLDER.set(locale);
        }
    }

    public static Locale get() {
        Locale locale = HOLDER.get();
        return locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
