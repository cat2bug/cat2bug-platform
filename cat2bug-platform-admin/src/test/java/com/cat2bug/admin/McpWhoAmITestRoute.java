package com.cat2bug.admin;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.framework.security.token.AccessTokenIdentities;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * 仅测试使用：在 MCP 路径下回显令牌解析出的用户，用来验证令牌鉴权链路。
 */
@ApplicationScoped
public class McpWhoAmITestRoute {

    void register(@Observes Router router) {
        router.get("/mcp/__whoami").handler(rc -> {
            LoginUser user = rc.user() instanceof QuarkusHttpUser http
                    ? AccessTokenIdentities.loginUser(http.getSecurityIdentity())
                    : null;
            if (user == null) {
                rc.response().setStatusCode(500).end("no login user");
                return;
            }
            rc.response().putHeader("Content-Type", "application/json").end(new JsonObject()
                    .put("userId", user.getUserId())
                    .put("userName", user.getUsername())
                    .encode());
        });
    }
}
