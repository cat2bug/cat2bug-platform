package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.LocaleUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.spring.SpringUtils;
import org.springframework.context.MessageSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 缺陷 Excel 导入：将各语言模板中的状态/类型显示名解析为枚举。
 */
public final class DefectImportLabelResolver
{
    private static volatile Map<String, SysDefectStateEnum> stateByLabel;
    private static volatile Map<String, SysDefectTypeEnum> typeByLabel;

    private DefectImportLabelResolver()
    {
    }

    public static SysDefectStateEnum resolveState(String label)
    {
        if (StringUtils.isBlank(label))
        {
            return null;
        }
        return stateByLabel().get(label.trim());
    }

    public static SysDefectTypeEnum resolveType(String label)
    {
        if (StringUtils.isBlank(label))
        {
            return null;
        }
        return typeByLabel().get(label.trim());
    }

    private static Map<String, SysDefectStateEnum> stateByLabel()
    {
        if (stateByLabel == null)
        {
            synchronized (DefectImportLabelResolver.class)
            {
                if (stateByLabel == null)
                {
                    stateByLabel = buildStateMap();
                }
            }
        }
        return stateByLabel;
    }

    private static Map<String, SysDefectTypeEnum> typeByLabel()
    {
        if (typeByLabel == null)
        {
            synchronized (DefectImportLabelResolver.class)
            {
                if (typeByLabel == null)
                {
                    typeByLabel = buildTypeMap();
                }
            }
        }
        return typeByLabel;
    }

    private static Map<String, SysDefectStateEnum> buildStateMap()
    {
        Map<String, SysDefectStateEnum> map = new HashMap<>();
        MessageSource messageSource = SpringUtils.getBean(MessageSource.class);
        for (SysDefectStateEnum state : SysDefectStateEnum.values())
        {
            map.put(state.name(), state);
            for (Locale locale : LocaleUtils.MESSAGE_BUNDLE_LOCALES)
            {
                try
                {
                    String msg = messageSource.getMessage(state.name(), null, locale);
                    if (StringUtils.isNotEmpty(msg))
                    {
                        map.put(msg.trim(), state);
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        }
        return map;
    }

    private static Map<String, SysDefectTypeEnum> buildTypeMap()
    {
        Map<String, SysDefectTypeEnum> map = new HashMap<>();
        MessageSource messageSource = SpringUtils.getBean(MessageSource.class);
        for (SysDefectTypeEnum type : SysDefectTypeEnum.values())
        {
            map.put(type.name(), type);
            for (Locale locale : LocaleUtils.MESSAGE_BUNDLE_LOCALES)
            {
                try
                {
                    String msg = messageSource.getMessage(type.name(), null, locale);
                    if (StringUtils.isNotEmpty(msg))
                    {
                        map.put(msg.trim(), type);
                    }
                }
                catch (Exception ignored)
                {
                }
            }
        }
        return map;
    }
}
