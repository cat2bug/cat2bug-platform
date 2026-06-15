package com.cat2bug.system.support;

import com.cat2bug.common.utils.spring.SpringUtils;
import org.springframework.context.MessageSource;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        when(messageSource.getMessage(anyString(), any(), any())).thenAnswer(inv -> "i18n:" + inv.getArgument(0));
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
