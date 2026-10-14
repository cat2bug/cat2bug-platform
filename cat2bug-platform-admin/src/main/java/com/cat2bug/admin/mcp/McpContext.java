package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.framework.security.token.AccessTokenIdentities;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.service.mcp.McpAccessService;
import com.cat2bug.system.service.mcp.McpProjectAccess;
import io.quarkiverse.mcp.server.McpConnection;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 工具调用入口：取调用人、限流、解析项目并校验「项目已开启 MCP + 模块已勾选 + 角色有权限」。
 */
@ApplicationScoped
public class McpContext {

    static final int MAX_CLIENT_NAME = 128;

    @Inject
    SecurityIdentity identity;

    @Inject
    McpRateLimiter rateLimiter;

    @Inject
    McpAccessService accessService;

    @Inject
    SysProjectMapper projectMapper;

    /** 只做身份与限流（不涉及具体项目的工具用）。 */
    public LoginUser caller() {
        LoginUser loginUser = identity == null || identity.isAnonymous()
                ? null : identity.getAttribute(AccessTokenIdentities.LOGIN_USER_ATTRIBUTE);
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new ToolCallException("未认证：请使用个人访问令牌或 OAuth 授权后再调用");
        }
        if (!rateLimiter.tryAcquire(loginUser.getUserId())) {
            throw new ToolCallException("调用过于频繁：每个用户每分钟最多 " + rateLimiter.perMinute() + " 次，请稍后再试");
        }
        return loginUser;
    }

    /**
     * @param anyOf 满足其一即可的权限；为空表示只要求项目已开启 MCP 且是项目成员
     */
    public McpScope scope(McpConnection connection, String projectRef, List<String> anyOf) {
        LoginUser caller = caller();
        Long projectId = resolveProjectId(projectRef);
        McpProjectAccess access = accessService.access(userOf(caller), projectId);
        if (access.project() == null || !(access.member() || access.admin())) {
            throw new ToolCallException("项目不存在或你不是该项目成员：" + projectRef);
        }
        if (!access.enabled()) {
            throw new ToolCallException("该项目未开启 MCP 访问，请项目管理员在「项目设置 → MCP」中开启");
        }
        if (anyOf != null && !anyOf.isEmpty() && !access.allowsAny(anyOf)) {
            throw new ToolCallException("无权限：需要 " + String.join(" 或 ", anyOf)
                    + "，且对应模块须在项目 MCP 设置中勾选");
        }
        return new McpScope(caller, clientName(connection), caller.getIpaddr(), access);
    }

    public McpScope scope(McpConnection connection, String projectRef, String... anyOf) {
        return scope(connection, projectRef, List.of(anyOf));
    }

    /** 已有 scope 下追加权限校验（例如先读对象再决定动作）。 */
    public void require(McpScope scope, String... anyOf) {
        if (!scope.access().allowsAny(List.of(anyOf))) {
            throw new ToolCallException("无权限：需要 " + String.join(" 或 ", anyOf)
                    + "，且对应模块须在项目 MCP 设置中勾选");
        }
    }

    Long resolveProjectId(String projectRef) {
        String ref = projectRef == null ? "" : projectRef.trim();
        if (ref.isEmpty()) {
            throw new ToolCallException("缺少 project 参数：传项目 ID，或「团队标识/项目标识」，可先调用 list_my_projects 查看");
        }
        if (ref.chars().allMatch(Character::isDigit)) {
            try {
                return Long.valueOf(ref);
            } catch (NumberFormatException e) {
                throw new ToolCallException("项目 ID 不合法：" + ref);
            }
        }
        int slash = ref.indexOf('/');
        if (slash <= 0 || slash == ref.length() - 1 || ref.indexOf('/', slash + 1) >= 0) {
            throw new ToolCallException("project 格式应为项目 ID 或「团队标识/项目标识」：" + ref);
        }
        SysProject project = projectMapper.selectSysProjectByTeamAndProjectKey(
                ref.substring(0, slash).trim(), ref.substring(slash + 1).trim());
        if (project == null || project.getProjectId() == null) {
            throw new ToolCallException("项目不存在：" + ref);
        }
        return project.getProjectId();
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
