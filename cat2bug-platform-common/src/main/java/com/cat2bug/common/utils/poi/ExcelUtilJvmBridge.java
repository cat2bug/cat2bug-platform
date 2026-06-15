package com.cat2bug.common.utils.poi;

import java.util.Map;
import java.util.Set;

/**
 * JVM 专用：将动态列配置应用到 {@link ExcelUtil}（Native 构建排除）。
 */
public final class ExcelUtilJvmBridge
{
    private ExcelUtilJvmBridge()
    {
    }

    public static void apply(ExcelUtil<?> util, Map<String, Object> params, Map<String, String> propToField,
            Set<String> requiredFields, Set<String> templateExcludedProps)
    {
        if (util == null || params == null || propToField == null)
        {
            return;
        }
        var orderedFieldNames = ExcelColumnExportSupport.resolveOrderedFieldNames(params, propToField, requiredFields,
                templateExcludedProps);
        if (!orderedFieldNames.isEmpty())
        {
            util.includeFields(orderedFieldNames.toArray(new String[0]));
        }
    }
}
