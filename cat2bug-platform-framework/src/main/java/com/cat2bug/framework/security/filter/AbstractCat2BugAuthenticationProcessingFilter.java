package com.cat2bug.framework.security.filter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-09-18 02:15
 * @Version: 1.0.0
 */
public abstract class AbstractCat2BugAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    protected AbstractCat2BugAuthenticationProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
        super(requiresAuthenticationRequestMatcher);
    }

    /**
     * 加入指定安全策略
     * @param httpSecurity
     */
    public abstract void joinHttpSecurity(HttpSecurity httpSecurity);

    /**
     * 获取匹配网址
     * @return
     */
    public abstract String[] getMatchers();
}
