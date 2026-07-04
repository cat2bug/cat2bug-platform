package com.cat2bug.admin.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesEncodingTest
{
    @Test
    void loadsSimplifiedChineseMessagesAsUtf8()
    {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

        String message = messageSource.getMessage("user.not.exists", null, Locale.SIMPLIFIED_CHINESE);
        assertTrue(message.contains("成员"), () -> "unexpected message: " + message);
    }

    @Test
    void loadsSetupWizardMessagesAsUtf8()
    {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

        String message = messageSource.getMessage("setup.test.connection.success", null, Locale.SIMPLIFIED_CHINESE);
        assertTrue(message.contains("连接"), () -> "unexpected message: " + message);
    }
}
