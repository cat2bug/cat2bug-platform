package com.cat2bug.framework.config;

import com.cat2bug.common.utils.StringUtils;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
        Locale locale;
        String language = request.getHeader(LANG);
        //中文language=zh_CN
        if (StringUtils.isNotEmpty(language)) {
            locale = new Locale(language);
        } else {
            locale = new Locale("zh_CN");
        }
        return locale;
    }

    @Override
    public void setLocale(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Locale locale) {

    }
}
