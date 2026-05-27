package com.cat2bug.framework.config;

import com.cat2bug.common.utils.LocaleUtils;
import com.cat2bug.common.utils.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-20 15:10
 * @Version: 1.0.0
 */
class MessageLocaleResolver  implements LocaleResolver {

    private static final String LANG = "language";

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String language = request.getHeader(LANG);
        if (StringUtils.isNotEmpty(language)) {
            return LocaleUtils.parseLanguageHeader(language);
        }
        return Locale.SIMPLIFIED_CHINESE;
    }

    @Override
    public void setLocale(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Locale locale) {

    }
}
