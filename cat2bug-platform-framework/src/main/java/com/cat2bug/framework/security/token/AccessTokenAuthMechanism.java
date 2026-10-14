package com.cat2bug.framework.security.token;

import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.framework.web.ClientInfo;
import com.cat2bug.framework.web.PublicUrlResolver;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

/**
 * MCP 路径上的 Bearer 令牌鉴权（个人访问令牌 {@code c2b_pat_} 与 OAuth 访问令牌 {@code c2b_oat_}）。
 * 其它路径直接返回匿名，网页接口仍由 JAX-RS {@code JwtAuthFilter} 处理。
 */
@ApplicationScoped
public class AccessTokenAuthMechanism implements HttpAuthenticationMechanism {

    static final String CLIENT_IP_ATTRIBUTE = "cat2bug.clientIp";
    static final String RESOURCE_ATTRIBUTE = "cat2bug.mcpResource";
    public static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource";

    private static final String BEARER = "Bearer ";

    @Inject
    PublicUrlResolver publicUrlResolver;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        if (!McpPaths.isMcp(context.normalizedPath())) {
            return Uni.createFrom().nullItem();
        }
        String token = bearerToken(context.request().getHeader("Authorization"));
        if (!AccessTokens.looksLikeAccessToken(token)) {
            return Uni.createFrom().nullItem();
        }
        TokenAuthenticationRequest request = new TokenAuthenticationRequest(new TokenCredential(token, "bearer"));
        request.setAttribute(CLIENT_IP_ATTRIBUTE, ClientInfo.resolveIp(context.request()));
        request.setAttribute(RESOURCE_ATTRIBUTE, publicUrlResolver.resolve(context.request()) + McpPaths.ROOT);
        HttpSecurityUtils.setRoutingContextAttribute(request, context);
        return identityProviderManager.authenticate(request);
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        String metadata = publicUrlResolver.resolve(context.request()) + PROTECTED_RESOURCE_METADATA_PATH;
        return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate",
                "Bearer realm=\"cat2bug\", resource_metadata=\"" + metadata + "\""));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of(TokenAuthenticationRequest.class);
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "bearer"));
    }

    static String bearerToken(String authorization) {
        if (authorization == null || authorization.length() <= BEARER.length()
                || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        return authorization.substring(BEARER.length()).trim();
    }
}
