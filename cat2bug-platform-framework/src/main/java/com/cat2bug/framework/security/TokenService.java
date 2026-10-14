package com.cat2bug.framework.security;

import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.common.constant.TokenConstants;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.framework.web.ClientInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 与 Spring {@code TokenService} 语义对齐：JWT 仅携带 session id，会话存 Redis/J2Cache。
 */
@ApplicationScoped
public class TokenService {

    private static final long MILLIS_MINUTE = 60_000L;
    private static final long MILLIS_MINUTE_TEN = 20 * 60 * 1000L;
    private static final int HS512_MIN_KEY_BYTES = 64;

    @ConfigProperty(name = "cat2bug.token.secret", defaultValue = "abcdefghijklmnopqrstuvwxyz")
    String secret;

    @ConfigProperty(name = "cat2bug.token.expire-minutes", defaultValue = "240")
    int expireMinutes;

    @Inject
    SessionStore sessionStore;

    public String createToken(LoginUser loginUser) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        loginUser.setToken(sessionId);
        ClientInfo client = ClientInfo.current();
        loginUser.setIpaddr(client.ip());
        loginUser.setLoginLocation(ClientInfo.resolveLocation(client.ip()));
        loginUser.setBrowser(client.browser());
        loginUser.setOs(client.os());
        refreshToken(loginUser);
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstants.LOGIN_USER_KEY, sessionId);
        return createJwt(claims);
    }

    public LoginUser getLoginUser(String authorizationHeader) {
        String jwt = stripBearer(authorizationHeader);
        if (jwt == null || jwt.isBlank()) {
            return null;
        }
        try {
            Claims claims = parseClaims(jwt);
            String sessionId = claims.get(TokenConstants.LOGIN_USER_KEY, String.class);
            if (sessionId == null || sessionId.isBlank()) {
                return null;
            }
            return sessionStore.get(tokenKey(sessionId));
        } catch (Exception ex) {
            return null;
        }
    }

    public void verifyToken(LoginUser loginUser) {
        if (loginUser == null || loginUser.getExpireTime() == null) {
            return;
        }
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
            refreshToken(loginUser);
        }
    }

    public void delLoginUser(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessionStore.delete(tokenKey(sessionId));
    }

    public List<LoginUser> listOnlineUsers() {
        return sessionStore.listActive(CacheConstants.LOGIN_TOKEN_KEY);
    }

    public void updateLoginUser(LoginUser loginUser) {
        if (loginUser == null || loginUser.getToken() == null) {
            return;
        }
        refreshToken(loginUser);
    }

    public LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUser(user);
        return loginUser;
    }

    public String stripBearer(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        if (authorizationHeader.startsWith(TokenConstants.TOKEN_PREFIX)) {
            return authorizationHeader.substring(TokenConstants.TOKEN_PREFIX.length());
        }
        return authorizationHeader;
    }

    public String sessionIdFromAuthorization(String authorizationHeader) {
        String jwt = stripBearer(authorizationHeader);
        if (jwt == null || jwt.isBlank()) {
            return null;
        }
        try {
            Claims claims = parseClaims(jwt);
            return claims.get(TokenConstants.LOGIN_USER_KEY, String.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void refreshToken(LoginUser loginUser) {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(System.currentTimeMillis() + expireMinutes * MILLIS_MINUTE);
        sessionStore.save(tokenKey(loginUser.getToken()), loginUser, expireMinutes * 60L);
    }

    private String tokenKey(String sessionId) {
        return CacheConstants.LOGIN_TOKEN_KEY + sessionId;
    }

    private String createJwt(Map<String, Object> claims) {
        return Jwts.builder()
                // 显式指定序列化器：Quarkus Native 不注册 ServiceLoader 实现
                .json(new JacksonSerializer<>())
                .claims(claims)
                .signWith(signingKey(), Jwts.SIG.HS512)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .json(new JacksonDeserializer<>())
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < HS512_MIN_KEY_BYTES) {
            keyBytes = sha512(keyBytes);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static byte[] sha512(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
