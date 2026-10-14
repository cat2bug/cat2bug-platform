package com.cat2bug.admin.resource;

import com.cat2bug.framework.security.token.McpPaths;
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
 * RFC 9728 受保护资源元数据：MCP 客户端收到 401 后据此找到授权服务器。
 * 同时响应带 {@code /mcp} 后缀的路径（规范允许客户端按资源路径拼接）。
 */
@Path("/.well-known/oauth-protected-resource")
@Produces(MediaType.APPLICATION_JSON)
public class OAuthProtectedResourceMetadataResource {

    @Inject
    PublicUrlResolver publicUrlResolver;

    @GET
    public Map<String, Object> metadata(@Context HttpServerRequest request) {
        String base = publicUrlResolver.resolve(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resource", base + McpPaths.ROOT);
        body.put("authorization_servers", List.of(base));
        body.put("bearer_methods_supported", List.of("header"));
        body.put("scopes_supported", List.of(OAuthService.SCOPE));
        body.put("resource_name", "Cat2Bug");
        return body;
    }

    @GET
    @Path("/mcp")
    public Map<String, Object> mcpMetadata(@Context HttpServerRequest request) {
        return metadata(request);
    }
}
