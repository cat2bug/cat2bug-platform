package com.cat2bug.system.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.common.security.AccessTokenResolver;
import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.system.mapper.SysUserAccessTokenMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Date;
import java.util.Objects;

/**
 * OAuth 令牌行的构造，以及 MCP 端点上的访问令牌校验。
 */
@ApplicationScoped
public class SysUserAccessTokenService implements AccessTokenResolver {

    /** 同一令牌一分钟内重复使用不再写库 */
    static final long TOUCH_INTERVAL_MILLIS = 60_000L;

    @Inject
    SysUserAccessTokenMapper tokenMapper;

    @Inject
    SysUserService sysUserService;

    @Inject
    SysPermissionService sysPermissionService;

    public SysUserAccessToken newRow(Long userId, Long projectId, String name, String plaintext, String source,
                                     Date expiresAt, Date now) {
        SysUserAccessToken row = new SysUserAccessToken();
        row.setUserId(userId);
        row.setProjectId(projectId);
        row.setName(name);
        row.setTokenHash(AccessTokens.sha256Hex(plaintext));
        row.setTokenPrefix(AccessTokens.displayPrefix(plaintext));
        row.setSource(source);
        row.setExpiresAt(expiresAt);
        row.setCreateTime(now);
        return row;
    }

    @Override
    public Authenticated authenticate(String token, String clientIp, String expectedResource) {
        if (!AccessTokens.looksLikeAccessToken(token)) {
            return null;
        }
        SysUserAccessToken row = tokenMapper.selectByHash(AccessTokens.sha256Hex(token));
        Date now = new Date();
        if (row == null
                || !SysUserAccessToken.SOURCE_OAUTH.equals(row.getSource())
                || row.getProjectId() == null
                || row.getRevokedAt() != null
                || (row.getExpiresAt() != null && !row.getExpiresAt().after(now))
                || !resourceMatches(row.getResource(), expectedResource)) {
            return null;
        }
        SysUser user = activeUser(row.getUserId());
        if (user == null) {
            return null;
        }
        touchIfNeeded(row, clientIp, now);
        LoginUser loginUser = sysPermissionService.createLoginUser(user);
        loginUser.setIpaddr(clientIp);
        loginUser.setLoginTime(now.getTime());
        if (row.getExpiresAt() != null) {
            loginUser.setExpireTime(row.getExpiresAt().getTime());
        }
        return new Authenticated(loginUser, row.getProjectId());
    }

    static boolean resourceMatches(String bound, String expected) {
        if (bound == null || bound.isBlank() || expected == null || expected.isBlank()) {
            return true;
        }
        return trimSlash(bound).equalsIgnoreCase(trimSlash(expected));
    }

    private static String trimSlash(String s) {
        String v = s.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    /** 按用户名再查一次：该查询过滤已删除用户（del_flag），按 ID 的查询不过滤 */
    public SysUser activeUser(Long userId) {
        SysUser byId = userId == null ? null : sysUserService.selectUserById(userId);
        if (byId == null || "1".equals(byId.getStatus())) {
            return null;
        }
        SysUser byName = sysUserService.selectUserByUserName(byId.getUserName());
        if (byName == null || !Objects.equals(byName.getUserId(), userId) || "1".equals(byName.getStatus())) {
            return null;
        }
        return byName;
    }

    private void touchIfNeeded(SysUserAccessToken row, String clientIp, Date now) {
        boolean stale = row.getLastUsedAt() == null
                || now.getTime() - row.getLastUsedAt().getTime() >= TOUCH_INTERVAL_MILLIS
                || !Objects.equals(row.getLastUsedIp(), clientIp);
        if (stale) {
            tokenMapper.touch(row.getTokenId(), now, clientIp);
        }
    }
}
