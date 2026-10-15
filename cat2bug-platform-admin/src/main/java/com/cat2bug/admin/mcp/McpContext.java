package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.framework.security.token.AccessTokenIdentities;
import com.cat2bug.system.service.mcp.McpAccessService;
import com.cat2bug.system.service.mcp.McpProjectAccess;
import io.quarkiverse.mcp.server.McpConnection;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 工具调用入口：取调用人与授权时绑定的项目、限流，并校验「项目已开启 MCP + 模块已勾选 + 角色有权限」。
 */
@ApplicationScoped
public class McpContext {

    static final int MAX_CLIENT_NAME = 128;
    static final String REAUTHORIZE_HINT = "请联系项目管理员，或在 AI 客户端中重新登录授权并选择其他项目";

    @Inject
    SecurityIdentity identity;

    @Inject
    McpRateLimiter rateLimiter;

    @Inject
    McpAccessService accessService;

    /** 只做身份与限流。 */
    public LoginUser caller() {
        LoginUser loginUser = AccessTokenIdentities.loginUser(identity);
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ToolCallException("未认证：请在 AI 客户端中登录 Cat2Bug 授权后再调用");
        }
        if (!rateLimiter.tryAcquire(loginUser.getUserId())) {
            throw new ToolCallException("调用过于频繁：每个用户每分钟最多 " + rateLimiter.perMinute() + " 次，请稍后再试");
        }
        return loginUser;
    }

    /** 授权时选择的项目。 */
    public Long projectId() {
        Long projectId = AccessTokenIdentities.projectId(identity);
        if (projectId == null) {
            throw new ToolCallException("当前授权未绑定项目：请在 AI 客户端中重新登录授权");
        }
        return projectId;
    }

    /**
     * @param anyOf 满足其一即可的权限；为空表示只要求项目已开启 MCP 且是项目成员
     */
    public McpScope scope(McpConnection connection, List<String> anyOf) {
        LoginUser caller = caller();
        McpProjectAccess access = accessService.access(userOf(caller), projectId());
        if (access.project() == null || !(access.member() || access.admin())) {
            throw new ToolCallException("授权的项目不存在或你已不是该项目成员。" + REAUTHORIZE_HINT);
        }
        if (!access.enabled()) {
            throw new ToolCallException("授权的项目已关闭 MCP 访问。" + REAUTHORIZE_HINT);
        }
        if (anyOf != null && !anyOf.isEmpty() && !access.allowsAny(anyOf)) {
            throw new ToolCallException("无权限：需要 " + String.join(" 或 ", anyOf)
                    + "，且对应模块须在项目 MCP 设置中勾选");
        }
        return new McpScope(caller, clientName(connection), caller.getIpaddr(), access);
    }

    public McpScope scope(McpConnection connection, String... anyOf) {
        return scope(connection, List.of(anyOf));
    }

    /** 已有 scope 下追加权限校验（例如先读对象再决定动作）。 */
    public void require(McpScope scope, String... anyOf) {
        if (!scope.access().allowsAny(List.of(anyOf))) {
            throw new ToolCallException("无权限：需要 " + String.join(" 或 ", anyOf)
                    + "，且对应模块须在项目 MCP 设置中勾选");
        }
    }

    static SysUser userOf(LoginUser loginUser) {
        SysUser user = loginUser.getUser();
        if (user != null && user.getUserId() != null) {
            return user;
        }
        SysUser stub = new SysUser();
        stub.setUserId(loginUser.getUserId());
        return stub;
    }

    static String clientName(McpConnection connection) {
        try {
            String name = connection == null || connection.initialRequest() == null
                    || connection.initialRequest().implementation() == null
                    ? null : connection.initialRequest().implementation().name();
            if (name == null || name.isBlank()) {
                return "unknown";
            }
            String trimmed = name.trim();
            return trimmed.length() > MAX_CLIENT_NAME ? trimmed.substring(0, MAX_CLIENT_NAME) : trimmed;
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
