package com.cat2bug.common.security;

import com.cat2bug.common.domain.LoginUser;

/**
 * 按个人访问令牌 / OAuth 访问令牌解析登录用户。令牌不存在、过期、吊销或用户不可用时返回 {@code null}。
 */
public interface AccessTokenResolver {

    default LoginUser authenticate(String token, String clientIp) {
        return authenticate(token, clientIp, null);
    }

    /**
     * @param expectedResource 本服务的 MCP 资源地址；OAuth 令牌绑定了别的 resource 时拒绝，为 {@code null} 时不校验
     */
    LoginUser authenticate(String token, String clientIp, String expectedResource);
}
