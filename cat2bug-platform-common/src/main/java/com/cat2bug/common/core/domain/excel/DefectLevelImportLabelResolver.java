package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.entity.SysDictData;
import com.cat2bug.common.utils.DictUtils;
import com.cat2bug.common.utils.LocaleUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.spring.SpringUtils;
import org.springframework.context.MessageSource;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 缺陷 Excel 导入：将各语言模板中的优先级显示名解析为字典值（urgent/height/middle/low）。
 */
public final class DefectLevelImportLabelResolver
{
    private static volatile Map<String, String> levelByLabel;

    private DefectLevelImportLabelResolver()
    {
    }

    public static String resolve(String label)
    {
        if (StringUtils.isBlank(label))
        {
            return null;
        }
        return levelByLabel().get(label.trim());
    }

    static Map<String, String> buildLevelMap(MessageSource messageSource, List<SysDictData> dictDatas)
    {
        Map<String, String> map = new HashMap<>();
        if (dictDatas == null)
        {
            return map;
        }
        for (SysDictData dict : dictDatas)
        {
            String dictValue = dict.getDictValue();
            if (StringUtils.isEmpty(dictValue))
            {
                continue;
            }
            putLabel(map, dictValue, dictValue);
            putLabel(map, dict.getDictLabel(), dictValue);
            if (messageSource != null)
            {
                for (Locale locale : LocaleUtils.MESSAGE_BUNDLE_LOCALES)
                {
                    try
                    {
                        String msg = messageSource.getMessage(dictValue, null, locale);
                        putLabel(map, msg, dictValue);
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }
        }
        return map;
    }

    private static void putLabel(Map<String, String> map, String label, String dictValue)
    {
        if (StringUtils.isNotEmpty(label))
        {
            map.put(label.trim(), dictValue);
        }
    }

    private static Map<String, String> levelByLabel()
    {
        if (levelByLabel == null)
        {
            synchronized (DefectLevelImportLabelResolver.class)
            {
                if (levelByLabel == null)
                {
                    MessageSource messageSource = SpringUtils.getBean(MessageSource.class);
                    List<SysDictData> dictDatas = DictUtils.getDictCache("defect_level");
                    levelByLabel = buildLevelMap(messageSource, dictDatas);
                }
            }
        }
        return levelByLabel;
    }
}
