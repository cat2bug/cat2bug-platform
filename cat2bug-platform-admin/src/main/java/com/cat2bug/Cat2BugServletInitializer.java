package com.cat2bug;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

/**
 * web容器中进行部署
 * 
 * @author ruoyi
 */
public class Cat2BugServletInitializer extends SpringBootServletInitializer
{
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
    {
        return application.sources(Cat2BugApplication.class);
    }
}
