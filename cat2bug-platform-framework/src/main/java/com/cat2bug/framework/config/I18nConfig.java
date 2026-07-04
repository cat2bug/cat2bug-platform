package com.cat2bug.framework.config;

import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
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
public class I18nConfig implements WebMvcConfigurer {
    @Bean
    public MessageSource messageSource(MessageSourceProperties properties)
    {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        if (StringUtils.hasText(properties.getBasename()))
        {
            messageSource.setBasenames(StringUtils.commaDelimitedListToStringArray(
                    StringUtils.trimAllWhitespace(properties.getBasename())));
        }
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
        messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver()
    {
        return new MessageLocaleResolver();
//        SessionLocaleResolver slr = new SessionLocaleResolver();
//        // 默认语言
//        slr.setDefaultLocale(Locale.CHINA);
//        return slr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor()
    {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        // 参数名
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
