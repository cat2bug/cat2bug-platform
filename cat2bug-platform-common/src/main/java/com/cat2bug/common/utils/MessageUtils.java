package com.cat2bug.common.utils;

import com.cat2bug.common.utils.spring.SpringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 获取i18n资源文件
 * 
 * @author ruoyi
 */
public class MessageUtils
{
    /**
     * 根据消息键和参数 获取消息 委托给spring messageSource
     *
     * @param code 消息键
     * @param args 参数
     * @return 获取国际化翻译值
     */
    public static String message(String code, Object... args)
    {
        MessageSource messageSource = SpringUtils.getBean(MessageSource.class);
        String fallback = formatFallbackMessage(code, args);
        return messageSource.getMessage(code, args, fallback, LocaleContextHolder.getLocale());
    }

    private static String formatFallbackMessage(String code, Object... args)
    {
        if (args == null || args.length == 0)
        {
            return code;
        }
        StringBuilder builder = new StringBuilder(code).append(": ");
        for (int i = 0; i < args.length; i++)
        {
            if (i > 0)
            {
                builder.append(", ");
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
