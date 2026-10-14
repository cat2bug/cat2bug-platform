package com.cat2bug.framework.security;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.security.LoginUserPermissionEnricher;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
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

    @Inject
    CurrentVertxRequest currentVertxRequest;

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
        if (loginUser.isMcpSession() && !fromLoopback()) {
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
        if (loginUser == null || loginUser.isMcpSession()) {
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

    /** MCP 内部会话只由本进程经回环地址转发使用，取 TCP 对端地址判断，不看 X-Forwarded-For。 */
    private boolean fromLoopback() {
        RoutingContext rc = currentVertxRequest == null ? null : currentVertxRequest.getCurrent();
        SocketAddress remote = rc == null ? null : rc.request().remoteAddress();
        String host = remote == null ? null : remote.hostAddress();
        if (host == null) {
            return false;
        }
        return host.equals("127.0.0.1") || host.equals("::1") || host.equals("0:0:0:0:0:0:0:1");
    }
}
