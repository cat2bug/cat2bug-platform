package com.cat2bug.common.util;

import com.cat2bug.common.domain.ModulePageSettings;

/**
 * 请求线程上的团队显示名。由闸门过滤器写入，响应结束时清除。
 */
public final class TeamModuleLabelContext {

    private static final ThreadLocal<ModulePageSettings> CURRENT = new ThreadLocal<>();

    private TeamModuleLabelContext() {
    }

    public static void set(ModulePageSettings settings) {
        if (settings == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(settings);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static ModulePageSettings current() {
        return CURRENT.get();
    }

    /** 根键有别名时返回别名，否则 {@code null}。 */
    public static String exact(String messageKey) {
        ModulePageSettings settings = CURRENT.get();
        if (settings == null || messageKey == null) {
            return null;
        }
        return settings.alias(messageKey);
    }

    public static String apply(String text) {
        return ModulePageSettingsSupport.applyVisible(text, CURRENT.get());
    }
}
