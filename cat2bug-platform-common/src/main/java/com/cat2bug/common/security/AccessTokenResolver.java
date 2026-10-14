package com.cat2bug.common.security;

import com.cat2bug.common.domain.LoginUser;

/**
 * 按个人访问令牌 / OAuth 访问令牌解析登录用户。令牌不存在、过期、吊销或用户不可用时返回 {@code null}。
 */
public interface AccessTokenResolver {

    LoginUser authenticate(String token, String clientIp);
}
