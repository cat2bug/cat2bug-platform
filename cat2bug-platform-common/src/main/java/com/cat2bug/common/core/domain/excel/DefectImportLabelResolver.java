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
        putStateAlias(map, "处理中", SysDefectStateEnum.PROCESSING);
        putStateAlias(map, "Processing", SysDefectStateEnum.PROCESSING);
        putStateAlias(map, "待验证", SysDefectStateEnum.AUDIT);
        putStateAlias(map, "Audit", SysDefectStateEnum.AUDIT);
        putStateAlias(map, "已解决", SysDefectStateEnum.RESOLVED);
        putStateAlias(map, "Resolved", SysDefectStateEnum.RESOLVED);
        putStateAlias(map, "已驳回", SysDefectStateEnum.REJECTED);
        putStateAlias(map, "Rejected", SysDefectStateEnum.REJECTED);
        putStateAlias(map, "已关闭", SysDefectStateEnum.CLOSED);
        putStateAlias(map, "Close", SysDefectStateEnum.CLOSED);
        return map;
    }

    private static void putStateAlias(Map<String, SysDefectStateEnum> map, String label, SysDefectStateEnum state)
    {
        if (StringUtils.isNotEmpty(label))
        {
            map.put(label.trim(), state);
        }
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
        putTypeAlias(map, "BUG", SysDefectTypeEnum.BUG);
        putTypeAlias(map, "Bug", SysDefectTypeEnum.BUG);
        putTypeAlias(map, "Task", SysDefectTypeEnum.TASK);
        putTypeAlias(map, "任务", SysDefectTypeEnum.TASK);
        putTypeAlias(map, "Demand", SysDefectTypeEnum.DEMAND);
        putTypeAlias(map, "需求", SysDefectTypeEnum.DEMAND);
        return map;
    }

    private static void putTypeAlias(Map<String, SysDefectTypeEnum> map, String label, SysDefectTypeEnum type)
    {
        if (StringUtils.isNotEmpty(label))
        {
            map.put(label.trim(), type);
        }
    }
}
