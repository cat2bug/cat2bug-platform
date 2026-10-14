package com.cat2bug.framework.security;

import com.cat2bug.common.domain.LoginUser;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * 在 JAX-RS 过滤器间传递 {@link LoginUser}，避免仅依赖 RequestScoped CDI 时出现空上下文。
 */
public final class SecurityRequestContext {

    public static final String LOGIN_USER_PROPERTY = "cat2bug.login-user";

    private SecurityRequestContext() {
    }

    public static void setLoginUser(ContainerRequestContext ctx, LoginUser loginUser) {
        if (ctx != null && loginUser != null) {
            ctx.setProperty(LOGIN_USER_PROPERTY, loginUser);
        }
    }

    public static LoginUser getLoginUser(ContainerRequestContext ctx) {
        if (ctx == null) {
            return null;
        }
        Object value = ctx.getProperty(LOGIN_USER_PROPERTY);
        return value instanceof LoginUser loginUser ? loginUser : null;
    }
}
