package com.cat2bug.framework.security;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.security.LoginUserPermissionEnricher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

@ApplicationScoped
public class LoginUserResolver {

    @Inject
    TokenService tokenService;

    @Inject
    AuthContext authContext;

    @Inject
    Instance<LoginUserPermissionEnricher> permissionEnrichers;

    public LoginUser resolve(ContainerRequestContext ctx) {
        LoginUser loginUser = SecurityRequestContext.getLoginUser(ctx);
        if (loginUser == null) {
            loginUser = authContext.getLoginUser();
        }
        if (loginUser == null && ctx != null) {
            loginUser = tokenService.getLoginUser(ctx.getHeaderString("Authorization"));
        }
        if (loginUser == null) {
            return null;
        }
        tokenService.verifyToken(loginUser);
        enrichIfNeeded(loginUser);
        SecurityRequestContext.setLoginUser(ctx, loginUser);
        authContext.setLoginUser(loginUser);
        return loginUser;
    }

    /** 按 token 字符串解析（不读请求头），用于 iframe 等只能携带 Cookie 的页面。 */
    public LoginUser resolveToken(String token) {
        LoginUser loginUser = tokenService.getLoginUser(token);
        if (loginUser == null) {
            return null;
        }
        tokenService.verifyToken(loginUser);
        enrichIfNeeded(loginUser);
        return loginUser;
    }

    private void enrichIfNeeded(LoginUser loginUser) {
        if (permissionEnrichers.isResolvable()) {
            permissionEnrichers.get().enrichIfNeeded(loginUser);
        }
    }
}
