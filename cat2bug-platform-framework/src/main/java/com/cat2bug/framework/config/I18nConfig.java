package com.cat2bug.framework.config;

import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-15 16:42
 * @Version: 1.0.0
 * 国际化
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer
{
    private static final String DEFAULT_MESSAGE_BASENAME = "classpath:i18n/messages";

    @Bean
    public MessageSource messageSource(Environment environment)
    {
        MessageSourceProperties properties = new MessageSourceProperties();
        Binder.get(environment).bind("spring.messages", Bindable.ofInstance(properties));

        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        String basename = StringUtils.hasText(properties.getBasename())
                ? properties.getBasename()
                : DEFAULT_MESSAGE_BASENAME;
        messageSource.setBasenames(toClasspathBasenames(basename));
        if (properties.getEncoding() != null)
        {
            messageSource.setDefaultEncoding(properties.getEncoding().name());
        }
        else
        {
            messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        }
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        Duration cacheDuration = properties.getCacheDuration();
        if (cacheDuration != null)
        {
            messageSource.setCacheMillis(cacheDuration.toMillis());
        }
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    private static String[] toClasspathBasenames(String basename)
    {
        String[] names = StringUtils.commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(basename));
        String[] resolved = new String[names.length];
        for (int i = 0; i < names.length; i++)
        {
            String name = names[i];
            resolved[i] = name.startsWith("classpath:") ? name : "classpath:" + name;
        }
        return resolved;
    }

    @Bean
    public LocaleResolver localeResolver()
    {
        return new MessageLocaleResolver();
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor()
    {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
