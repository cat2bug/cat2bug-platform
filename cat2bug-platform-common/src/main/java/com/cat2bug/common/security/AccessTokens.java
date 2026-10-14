package com.cat2bug.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 访问令牌、刷新令牌、授权码的生成与哈希。明文只在签发时出现一次，库里只存 SHA-256。
 */
public final class AccessTokens {

    public static final String PERSONAL_PREFIX = "c2b_pat_";
    public static final String OAUTH_ACCESS_PREFIX = "c2b_oat_";
    public static final String OAUTH_REFRESH_PREFIX = "c2b_ort_";
    public static final String OAUTH_CODE_PREFIX = "c2b_oac_";

    /** 展示前缀长度：类型前缀之后再露出 4 个随机字符，便于用户辨认 */
    static final int DISPLAY_RANDOM_CHARS = 4;

    private static final char[] BASE62 =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int RANDOM_CHARS = 43;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AccessTokens() {
    }

    public static String generate(String prefix) {
        StringBuilder sb = new StringBuilder(prefix.length() + RANDOM_CHARS).append(prefix);
        for (int i = 0; i < RANDOM_CHARS; i++) {
            sb.append(BASE62[RANDOM.nextInt(BASE62.length)]);
        }
        return sb.toString();
    }

    public static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256(value));
    }

    /** PKCE S256：BASE64URL(SHA256(verifier))，无填充 */
    public static String pkceChallenge(String verifier) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
    }

    public static String displayPrefix(String token) {
        if (token == null) {
            return "";
        }
        int typeEnd = token.startsWith("c2b_") && token.length() > 8 ? 8 : 0;
        return token.substring(0, Math.min(token.length(), typeEnd + DISPLAY_RANDOM_CHARS));
    }

    public static boolean looksLikeAccessToken(String token) {
        return token != null && (token.startsWith(PERSONAL_PREFIX) || token.startsWith(OAUTH_ACCESS_PREFIX));
    }

    /** 常量时间比较，避免按字节提前返回泄露信息 */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
