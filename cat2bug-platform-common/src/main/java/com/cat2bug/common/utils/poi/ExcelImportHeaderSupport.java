package com.cat2bug.common.utils.poi;

import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.utils.LocaleUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.spring.SpringUtils;
import org.springframework.context.MessageSource;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Excel 导入表头多语言候选（与当前请求 locale 无关）。
 */
public final class ExcelImportHeaderSupport
{
    private ExcelImportHeaderSupport()
    {
    }

    public static Set<String> collectImportHeaderCandidates(Excel attr)
    {
        String currentTitle = StringUtils.isNotEmpty(attr.i18nNameKey())
                && StringUtils.isNotEmpty(MessageUtils.message(attr.i18nNameKey()))
                ? MessageUtils.message(attr.i18nNameKey())
                : attr.name();
        MessageSource messageSource = null;
        try
        {
            messageSource = SpringUtils.getBean(MessageSource.class);
        }
        catch (Exception ignored)
        {
        }
        return collectImportHeaderCandidates(attr, messageSource, currentTitle);
    }

    static Set<String> collectImportHeaderCandidates(Excel attr, MessageSource messageSource, String currentTitle)
    {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (StringUtils.isNotEmpty(currentTitle))
        {
            candidates.add(currentTitle.trim());
        }
        if (StringUtils.isNotEmpty(attr.name()))
        {
            candidates.add(attr.name().trim());
        }
        addTranslationsForKey(candidates, messageSource, attr.i18nNameKey());
        if (attr.alternateI18nKeys() != null)
        {
            for (String altKey : attr.alternateI18nKeys())
            {
                addTranslationsForKey(candidates, messageSource, altKey);
            }
        }
        return candidates;
    }

    private static void addTranslationsForKey(Set<String> candidates, MessageSource messageSource, String key)
    {
        if (StringUtils.isEmpty(key) || messageSource == null)
        {
            return;
        }
        for (Locale locale : LocaleUtils.MESSAGE_BUNDLE_LOCALES)
        {
            try
            {
                String msg = messageSource.getMessage(key, null, locale);
                if (StringUtils.isNotEmpty(msg))
                {
                    candidates.add(msg.trim());
                }
            }
            catch (Exception ignored)
            {
            }
        }
    }
}
