package com.cat2bug.admin.security;

import com.cat2bug.common.util.LocaleUtils;
import com.cat2bug.common.util.MessageLocaleContext;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * 将请求头 {@code language} 写入 {@link MessageLocaleContext}，供 API 错误文案随界面语言切换。
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class MessageLocaleFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String language = requestContext.getHeaderString("language");
        MessageLocaleContext.set(LocaleUtils.parseLanguageHeader(language));
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MessageLocaleContext.clear();
    }
}
