package com.cat2bug.admin;

import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.framework.web.IpRateLimiter;
import com.cat2bug.framework.web.PublicUrlResolver;
import com.cat2bug.system.service.oauth.OAuthService;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokenUnitTest {

    @Test
    void generatedTokensHavePrefixLengthAndAreUnique() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String token = AccessTokens.generate(AccessTokens.OAUTH_ACCESS_PREFIX);
            assertTrue(token.startsWith("c2b_oat_"));
            assertEquals(8 + 43, token.length());
            assertTrue(token.substring(8).matches("[0-9A-Za-z]{43}"));
            assertTrue(seen.add(token));
        }
    }

    @Test
    void sha256AndDisplayPrefix() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", AccessTokens.sha256Hex("abc"));
        String token = AccessTokens.generate(AccessTokens.OAUTH_ACCESS_PREFIX);
        assertEquals(token.substring(0, 12), AccessTokens.displayPrefix(token));
        assertTrue(AccessTokens.looksLikeAccessToken(token));
        assertFalse(AccessTokens.looksLikeAccessToken("c2b_pat_" + "a".repeat(43)));
        assertFalse(AccessTokens.looksLikeAccessToken(AccessTokens.generate(AccessTokens.OAUTH_REFRESH_PREFIX)));
        assertFalse(AccessTokens.looksLikeAccessToken("eyJhbGciOiJIUzUxMiJ9.x.y"));
    }

    @Test
    void pkceMatchesRfc7636Vector() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                AccessTokens.pkceChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
    }

    @Test
    void redirectUriRules() {
        assertTrue(OAuthService.isAllowedRedirectUri("https://claude.ai/api/mcp/auth_callback"));
        assertTrue(OAuthService.isAllowedRedirectUri("http://127.0.0.1:33418/callback"));
        assertTrue(OAuthService.isAllowedRedirectUri("http://localhost:6274/oauth/callback"));
        assertTrue(OAuthService.isAllowedRedirectUri("http://[::1]:8080/cb"));
        assertFalse(OAuthService.isAllowedRedirectUri("http://evil.example.com/cb"));
        assertFalse(OAuthService.isAllowedRedirectUri("https://a.example.com/cb#frag"));
        assertFalse(OAuthService.isAllowedRedirectUri("https://user:pw@a.example.com/cb"));
        assertFalse(OAuthService.isAllowedRedirectUri("javascript:alert(1)"));
        assertFalse(OAuthService.isAllowedRedirectUri("cursor://callback"));
        assertFalse(OAuthService.isAllowedRedirectUri(""));
    }

    @Test
    void publicUrlDerivation() {
        assertEquals("https://bug.example.com", PublicUrlResolver.derive("https", "bug.example.com", "10.0.0.2:2020", "http"));
        assertEquals("https://a.example.com", PublicUrlResolver.derive("https, http", "a.example.com, b", null, "http"));
        assertEquals("http://localhost:2020", PublicUrlResolver.derive(null, null, "localhost:2020", "http"));
        assertEquals("http://127.0.0.1", PublicUrlResolver.derive(null, null, null, null));
    }

    @Test
    void rateLimiterWindow() {
        AtomicLong now = new AtomicLong(1_000_000L);
        IpRateLimiter limiter = new IpRateLimiter(now::get);
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire("b", "1.1.1.1", 3));
        }
        assertFalse(limiter.tryAcquire("b", "1.1.1.1", 3));
        assertTrue(limiter.tryAcquire("b", "2.2.2.2", 3));
        assertTrue(limiter.tryAcquire("other", "1.1.1.1", 3));
        now.addAndGet(60_001L);
        assertTrue(limiter.tryAcquire("b", "1.1.1.1", 3));
    }

    @Test
    void proxyAddressDetection() {
        assertTrue(IpRateLimiter.isProxyAddress("127.0.0.1"));
        assertTrue(IpRateLimiter.isProxyAddress("10.1.2.3"));
        assertTrue(IpRateLimiter.isProxyAddress("192.168.1.10"));
        assertTrue(IpRateLimiter.isProxyAddress("0:0:0:0:0:0:0:1"));
        assertFalse(IpRateLimiter.isProxyAddress("8.8.8.8"));
        assertFalse(IpRateLimiter.isProxyAddress("example.com"));
    }
}
