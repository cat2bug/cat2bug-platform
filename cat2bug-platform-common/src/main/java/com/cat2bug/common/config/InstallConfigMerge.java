package com.cat2bug.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安装配置 YAML 树合并工具。
 */
public final class InstallConfigMerge {

    private InstallConfigMerge() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) nested));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    public static void deepMerge(Map<String, Object> base, Map<String, Object> overrides) {
        if (base == null || overrides == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : overrides.entrySet()) {
            String key = entry.getKey();
            Object overrideValue = entry.getValue();
            Object existing = base.get(key);
            if (existing instanceof Map<?, ?> existingMap && overrideValue instanceof Map<?, ?> overrideMap) {
                deepMerge((Map<String, Object>) existingMap, (Map<String, Object>) overrideMap);
            } else {
                base.put(key, overrideValue);
            }
        }
    }
}
