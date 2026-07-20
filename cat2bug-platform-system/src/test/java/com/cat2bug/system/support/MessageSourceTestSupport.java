package com.cat2bug.system.support;

import com.cat2bug.common.utils.spring.SpringUtils;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 无 Spring 容器的单元测试：mock MessageSource，避免 SpringUtils.beanFactory 为 null。
 */
public final class MessageSourceTestSupport {

    private MessageSourceTestSupport() {
    }

    /** @return tearDown 时调用的清理回调 */
    public static Runnable installMessageSource() {
        MessageSource messageSource = mock(MessageSource.class);
        // MessageUtils 走 4 参数重载：getMessage(code, args, defaultMessage, locale)
        when(messageSource.getMessage(anyString(), nullable(Object[].class), anyString(), any(Locale.class)))
                .thenAnswer(inv -> inv.getArgument(2));
        when(messageSource.getMessage(anyString(), nullable(Object[].class), isNull(), any(Locale.class)))
                .thenAnswer(inv -> "i18n:" + inv.getArgument(0));
        // 兼容仅传 locale 的 3 参数重载
        when(messageSource.getMessage(anyString(), nullable(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> "i18n:" + inv.getArgument(0));
        SpringUtils.registerTestBean(MessageSource.class, messageSource);
        return SpringUtils::clearTestBeans;
    }

    public static void runWithMessages(Runnable action) {
        Runnable cleanup = installMessageSource();
        try {
            action.run();
        } finally {
            cleanup.run();
        }
    }

    public static <T> T withMessages(Supplier<T> action) {
        Runnable cleanup = installMessageSource();
        try {
            return action.get();
        } finally {
            cleanup.run();
        }
    }
}
