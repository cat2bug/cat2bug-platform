package com.cat2bug.admin.resource;

import com.cat2bug.framework.web.PublicUrlResolver;
import com.cat2bug.system.service.oauth.OAuthService;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RFC 8414 授权服务器元数据。
 */
@Path("/.well-known/oauth-authorization-server")
@Produces(MediaType.APPLICATION_JSON)
public class OAuthMetadataResource {

    @Inject
    PublicUrlResolver publicUrlResolver;

    @GET
    public Map<String, Object> metadata(@Context HttpServerRequest request) {
        String issuer = publicUrlResolver.resolve(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("issuer", issuer);
        body.put("authorization_endpoint", issuer + "/oauth/authorize");
        body.put("token_endpoint", issuer + "/oauth/token");
        body.put("registration_endpoint", issuer + "/oauth/register");
        body.put("revocation_endpoint", issuer + "/oauth/revoke");
        body.put("response_types_supported", List.of("code"));
        body.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
        body.put("code_challenge_methods_supported", List.of("S256"));
        body.put("token_endpoint_auth_methods_supported", List.of("none"));
        body.put("revocation_endpoint_auth_methods_supported", List.of("none"));
        body.put("scopes_supported", List.of(OAuthService.SCOPE));
        return body;
    }
}
