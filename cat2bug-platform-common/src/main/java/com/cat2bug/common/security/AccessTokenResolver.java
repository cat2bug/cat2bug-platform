package com.cat2bug.common.security;

import com.cat2bug.common.domain.LoginUser;

/**
 * 按 OAuth 访问令牌解析登录用户与授权时选择的项目。令牌不存在、过期、吊销、未绑定项目或用户不可用时返回 {@code null}。
 */
public interface AccessTokenResolver {

    record Authenticated(LoginUser loginUser, Long projectId) {
    }

    default Authenticated authenticate(String token, String clientIp) {
        return authenticate(token, clientIp, null);
    }

    /**
     * @param expectedResource 本服务的 MCP 资源地址；令牌绑定了别的 resource 时拒绝，为 {@code null} 时不校验
     */
    Authenticated authenticate(String token, String clientIp, String expectedResource);
}
