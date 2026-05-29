package com.cat2bug.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 升级配置合并器：向导覆盖 > 已有非空值 > 模板默认。
 */
public final class UpgradeConfigMerger
{
    private UpgradeConfigMerger()
    {
    }

    /**
     * 合并优先级（高 → 低）：{@code wizardOverrides}、{@code base} 非空值、{@code template} 默认。
     */
    public static Map<String, Object> mergePreserveExisting(Map<String, Object> base,
                                                             Map<String, Object> template,
                                                             Map<String, Object> wizardOverrides)
    {
        Map<String, Object> merged = InstallConfigMerge.deepCopy(template != null ? template : Map.of());
        if (base != null)
        {
            mergeKeepExisting(merged, base);
        }
        if (wizardOverrides != null)
        {
            InstallConfigMerge.deepMerge(merged, wizardOverrides);
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private static void mergeKeepExisting(Map<String, Object> target, Map<String, Object> source)
    {
        if (target == null || source == null)
        {
            return;
        }
        for (Map.Entry<String, Object> entry : source.entrySet())
        {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();
            Object targetValue = target.get(key);

            if (sourceValue instanceof Map<?, ?> sourceMap)
            {
                Map<String, Object> nextTarget;
                if (targetValue instanceof Map<?, ?> targetMap)
                {
                    nextTarget = (Map<String, Object>) targetMap;
                }
                else
                {
                    nextTarget = new LinkedHashMap<>();
                    target.put(key, nextTarget);
                }
                mergeKeepExisting(nextTarget, (Map<String, Object>) sourceMap);
                continue;
            }
            if (!isEmptyValue(sourceValue))
            {
                target.put(key, sourceValue);
            }
        }
    }

    private static boolean isEmptyValue(Object value)
    {
        if (value == null)
        {
            return true;
        }
        if (value instanceof String str)
        {
            return str.isBlank();
        }
        return false;
    }
}
