package com.cat2bug.framework.security.token;

import com.cat2bug.common.domain.LoginUser;
import io.quarkus.security.identity.SecurityIdentity;

/**
 * 从令牌鉴权得到的 {@link SecurityIdentity} 中取出平台登录用户。
 */
public final class AccessTokenIdentities {

    public static final String LOGIN_USER_ATTRIBUTE = "cat2bug.loginUser";

    private AccessTokenIdentities() {
    }

    public static LoginUser loginUser(SecurityIdentity identity) {
        if (identity == null || identity.isAnonymous()) {
            return null;
        }
        Object value = identity.getAttribute(LOGIN_USER_ATTRIBUTE);
        return value instanceof LoginUser loginUser ? loginUser : null;
    }
}
