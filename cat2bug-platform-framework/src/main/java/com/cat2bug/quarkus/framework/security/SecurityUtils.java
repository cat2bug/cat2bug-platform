package com.cat2bug.quarkus.framework.security;

import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.exception.ApiException;
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * 对齐 Spring SecurityUtils 静态 API，底层读 {@link AuthContext}。
 */
@ApplicationScoped
public class SecurityUtils {

    @Inject
    AuthContext authContext;

    public static Long getUserId() {
        LoginUser user = requireLoginUser();
        return user.getUserId();
    }

    public static String getUsername() {
        LoginUser user = requireLoginUser();
        return user.getUsername();
    }

    public static LoginUser getLoginUser() {
        return requireLoginUser();
    }

    public static boolean isAdmin() {
        LoginUser user = auth().getLoginUser();
        if (user == null || user.getUser() == null) {
            return false;
        }
        return user.getUser().isAdmin();
    }

    private static LoginUser requireLoginUser() {
        LoginUser user = auth().getLoginUser();
        if (user == null) {
            throw new ApiException(401, "未登录");
        }
        return user;
    }

    private static AuthContext auth() {
        AuthContext ctx = Arc.container().instance(AuthContext.class).get();
        if (ctx == null) {
            throw new ApiException(500, "AuthContext 不可用");
        }
        return ctx;
    }
}
