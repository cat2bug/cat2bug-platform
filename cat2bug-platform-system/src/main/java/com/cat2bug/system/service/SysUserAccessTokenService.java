package com.cat2bug.system.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.security.AccessTokenResolver;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.system.mapper.SysUserAccessTokenMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 个人访问令牌的签发、列表、吊销，以及 MCP 端点上的令牌校验。
 */
@ApplicationScoped
public class SysUserAccessTokenService implements AccessTokenResolver {

    static final int MAX_NAME_LENGTH = 128;
    static final int MAX_EXPIRE_DAYS = 3650;
    /** 同一令牌一分钟内重复使用不再写库 */
    static final long TOUCH_INTERVAL_MILLIS = 60_000L;

    private static final Set<String> AUTHENTICATING_SOURCES =
            Set.of(SysUserAccessToken.SOURCE_MANUAL, SysUserAccessToken.SOURCE_OAUTH);

    @Inject
    SysUserAccessTokenMapper tokenMapper;

    @Inject
    SysUserService sysUserService;

    @Inject
    SysPermissionService sysPermissionService;

    public record CreatedToken(SysUserAccessToken token, String plaintext) {
    }

    public CreatedToken createPersonal(Long userId, String name, Integer expireDays) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new ApiException(MessageUtils.message("user.access-token.name-invalid", String.valueOf(MAX_NAME_LENGTH)));
        }
        if (expireDays != null && (expireDays < 1 || expireDays > MAX_EXPIRE_DAYS)) {
            throw new ApiException(MessageUtils.message("user.access-token.expire-days-invalid", String.valueOf(MAX_EXPIRE_DAYS)));
        }
        Date now = new Date();
        Date expiresAt = expireDays == null ? null : new Date(now.getTime() + expireDays * 86_400_000L);
        String plaintext = AccessTokens.generate(AccessTokens.PERSONAL_PREFIX);
        SysUserAccessToken row = newRow(userId, trimmed, plaintext, SysUserAccessToken.SOURCE_MANUAL, expiresAt, now);
        tokenMapper.insert(row);
        return new CreatedToken(row, plaintext);
    }

    public SysUserAccessToken newRow(Long userId, String name, String plaintext, String source, Date expiresAt, Date now) {
        SysUserAccessToken row = new SysUserAccessToken();
        row.setUserId(userId);
        row.setName(name);
        row.setTokenHash(AccessTokens.sha256Hex(plaintext));
        row.setTokenPrefix(AccessTokens.displayPrefix(plaintext));
        row.setSource(source);
        row.setExpiresAt(expiresAt);
        row.setCreateTime(now);
        return row;
    }

    public List<SysUserAccessToken> listVisible(Long userId) {
        return tokenMapper.selectVisibleByUserId(userId);
    }

    /** 只能吊销自己的令牌；OAuth 条目连带吊销同一授权下的访问令牌与刷新令牌 */
    public boolean revoke(Long userId, Long tokenId) {
        SysUserAccessToken row = tokenId == null ? null : tokenMapper.selectById(tokenId);
        if (row == null || !Objects.equals(row.getUserId(), userId)) {
            return false;
        }
        Date now = new Date();
        if (row.getGrantId() != null) {
            return tokenMapper.revokeGrant(row.getGrantId(), now) > 0;
        }
        return tokenMapper.revokeById(tokenId, now) > 0;
    }

    @Override
    public LoginUser authenticate(String token, String clientIp, String expectedResource) {
        if (!AccessTokens.looksLikeAccessToken(token)) {
            return null;
        }
        SysUserAccessToken row = tokenMapper.selectByHash(AccessTokens.sha256Hex(token));
        Date now = new Date();
        if (row == null
                || !AUTHENTICATING_SOURCES.contains(row.getSource())
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
        return loginUser;
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
        if (!stale) {
            return;
        }
        tokenMapper.touch(row.getTokenId(), now, clientIp);
        if (row.getGrantId() != null) {
            tokenMapper.touchGrantRefresh(row.getGrantId(), now, clientIp);
        }
    }
}
