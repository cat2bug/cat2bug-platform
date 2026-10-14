package com.cat2bug.quarkus.common.security;

import com.cat2bug.quarkus.common.domain.LoginUser;

/**
 * 会话中 permissions 缺失时从数据库补全（Redis 反序列化、旧会话兼容）。
 */
public interface LoginUserPermissionEnricher {

    void enrichIfNeeded(LoginUser loginUser);
}
