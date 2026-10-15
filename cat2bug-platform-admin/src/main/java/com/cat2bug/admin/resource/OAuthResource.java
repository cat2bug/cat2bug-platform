package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysOAuthClient;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.framework.web.IpRateLimiter;
import com.cat2bug.system.service.oauth.OAuthException;
import com.cat2bug.system.service.oauth.OAuthService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面向 MCP 客户端的 OAuth 2.1 端点。协议端点按 RFC 返回标准 JSON 与状态码，不包 AjaxResult；
 * 只有确认页调用的 {@code authorize/check}、{@code authorize/decision} 走网页登录态并返回 AjaxResult。
 */
@Path("/oauth")
public class OAuthResource {

    static final String CONSENT_PAGE = "/oauth/consent";

    @Inject
    OAuthService oauthService;

    @Inject
    IpRateLimiter rateLimiter;

    @ConfigProperty(name = "cat2bug.oauth.rate-limit.register-per-minute", defaultValue = "10")
    int registerPerMinute;

    @ConfigProperty(name = "cat2bug.oauth.rate-limit.token-per-minute", defaultValue = "60")
    int tokenPerMinute;

    public record RegisterRequest(@JsonProperty("client_name") String clientName,
                                  @JsonProperty("redirect_uris") List<String> redirectUris,
                                  @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod) {
    }

    public record AuthorizeBody(@JsonProperty("response_type") String responseType,
                                @JsonProperty("client_id") String clientId,
                                @JsonProperty("redirect_uri") String redirectUri,
                                @JsonProperty("code_challenge") String codeChallenge,
                                @JsonProperty("code_challenge_method") String codeChallengeMethod,
                                @JsonProperty("state") String state,
                                @JsonProperty("resource") String resource,
                                @JsonProperty("approve") Boolean approve,
                                @JsonProperty("project_id") Long projectId) {

        OAuthService.AuthorizeRequest toRequest() {
            return new OAuthService.AuthorizeRequest(responseType, clientId, redirectUri, codeChallenge,
                    codeChallengeMethod, state, resource);
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegisterRequest body, @Context HttpServerRequest http) {
        if (!rateLimiter.tryAcquire("oauth-register", IpRateLimiter.clientKey(http), registerPerMinute)) {
            return tooManyRequests();
        }
        if (body == null) {
            return error(new OAuthException("invalid_client_metadata", "request body is required"));
        }
        if (body.tokenEndpointAuthMethod() != null && !"none".equals(body.tokenEndpointAuthMethod())) {
            return error(new OAuthException("invalid_client_metadata", "only public clients (none) are supported"));
        }
        try {
            SysOAuthClient client = oauthService.register(body.clientName(), body.redirectUris());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("client_id", client.getClientId());
            result.put("client_id_issued_at", client.getCreateTime().getTime() / 1000);
            result.put("client_name", client.getClientName());
            result.put("redirect_uris", client.redirectUriList());
            result.put("token_endpoint_auth_method", "none");
            result.put("grant_types", List.of("authorization_code", "refresh_token"));
            result.put("response_types", List.of("code"));
            return Response.status(201).entity(result).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (OAuthException e) {
            return error(e);
        }
    }

    @GET
    @Path("/authorize")
    public Response authorize(@QueryParam("response_type") String responseType,
                              @QueryParam("client_id") String clientId,
                              @QueryParam("redirect_uri") String redirectUri,
                              @QueryParam("code_challenge") String codeChallenge,
                              @QueryParam("code_challenge_method") String codeChallengeMethod,
                              @QueryParam("state") String state,
                              @QueryParam("resource") String resource,
                              @Context UriInfo uriInfo) {
        OAuthService.AuthorizeRequest request = new OAuthService.AuthorizeRequest(responseType, clientId, redirectUri,
                codeChallenge, codeChallengeMethod, state, resource);
        try {
            oauthService.validate(request);
        } catch (OAuthException e) {
            if (e.getRedirectUri() == null) {
                return errorPage(e);
            }
            String target = OAuthService.errorRedirect(e.getRedirectUri(), e.getError(), e.getMessage(), state);
            return Response.status(302).location(URI.create(target)).build();
        }
        String query = uriInfo.getRequestUri().getRawQuery();
        return Response.status(302).location(URI.create(CONSENT_PAGE + (query == null ? "" : "?" + query))).build();
    }

    @POST
    @Path("/authorize/check")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AjaxResult check(AuthorizeBody body) {
        if (body == null) {
            return AjaxResult.error("invalid_request");
        }
        try {
            OAuthService.ValidatedAuthorize validated = oauthService.validate(body.toRequest());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("clientName", validated.client().getClientName());
            data.put("redirectUri", validated.redirectUri());
            data.put("redirectHost", URI.create(validated.redirectUri()).getHost());
            data.put("teams", selectableTeams(SecurityUtils.getUserId()));
            return AjaxResult.success(data);
        } catch (OAuthException e) {
            return AjaxResult.error(e.getError() + ": " + e.getMessage());
        }
    }

    @POST
    @Path("/authorize/decision")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AjaxResult decision(AuthorizeBody body) {
        if (body == null) {
            return AjaxResult.error("invalid_request");
        }
        boolean approve = Boolean.TRUE.equals(body.approve());
        if (approve && body.projectId() == null) {
            return AjaxResult.error("invalid_request: project_id is required");
        }
        try {
            String redirect = approve
                    ? oauthService.approve(SecurityUtils.getUserId(), body.projectId(), body.toRequest())
                    : oauthService.deny(body.toRequest());
            return AjaxResult.success(Map.of("redirect", redirect));
        } catch (OAuthException e) {
            return AjaxResult.error(e.getError() + ": " + e.getMessage());
        }
    }

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(@FormParam("grant_type") String grantType,
                          @FormParam("code") String code,
                          @FormParam("redirect_uri") String redirectUri,
                          @FormParam("client_id") String clientId,
                          @FormParam("code_verifier") String codeVerifier,
                          @FormParam("refresh_token") String refreshToken,
                          @FormParam("resource") String resource,
                          @Context HttpServerRequest http) {
        if (!rateLimiter.tryAcquire("oauth-token", IpRateLimiter.clientKey(http), tokenPerMinute)) {
            return tooManyRequests();
        }
        try {
            OAuthService.TokenResponse tokens;
            if ("authorization_code".equals(grantType)) {
                tokens = oauthService.exchangeCode(code, clientId, redirectUri, codeVerifier, resource);
            } else if ("refresh_token".equals(grantType)) {
                tokens = oauthService.refresh(refreshToken, clientId, resource);
            } else {
                throw new OAuthException("unsupported_grant_type", "grant_type must be authorization_code or refresh_token");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("access_token", tokens.accessToken());
            result.put("token_type", "Bearer");
            result.put("expires_in", tokens.expiresIn());
            result.put("refresh_token", tokens.refreshToken());
            result.put("scope", tokens.scope());
            return noStore(Response.ok(result, MediaType.APPLICATION_JSON_TYPE)).build();
        } catch (OAuthException e) {
            return error(e);
        }
    }

    @POST
    @Path("/revoke")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response revoke(@FormParam("token") String token, @Context HttpServerRequest http) {
        if (!rateLimiter.tryAcquire("oauth-token", IpRateLimiter.clientKey(http), tokenPerMinute)) {
            return tooManyRequests();
        }
        oauthService.revoke(token);
        return Response.ok().build();
    }

    /** 按团队分组的可选项目，保持查询顺序 */
    private List<Map<String, Object>> selectableTeams(Long userId) {
        Map<Long, Map<String, Object>> teams = new LinkedHashMap<>();
        for (SysProject p : oauthService.selectableProjects(userId)) {
            Map<String, Object> team = teams.computeIfAbsent(p.getTeamId(), id -> {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("teamId", id);
                t.put("teamName", p.getTeamName());
                t.put("teamKey", p.getTeamKey());
                t.put("projects", new ArrayList<Map<String, Object>>());
                return t;
            });
            Map<String, Object> project = new LinkedHashMap<>();
            project.put("projectId", p.getProjectId());
            project.put("projectName", p.getProjectName());
            project.put("projectKey", p.getProjectKey());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> projects = (List<Map<String, Object>>) team.get("projects");
            projects.add(project);
        }
        return new ArrayList<>(teams.values());
    }

    private static Response.ResponseBuilder noStore(Response.ResponseBuilder builder) {
        return builder.header("Cache-Control", "no-store").header("Pragma", "no-cache");
    }

    static Response error(OAuthException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", e.getError());
        body.put("error_description", e.getMessage());
        return noStore(Response.status(e.getHttpStatus()).entity(body).type(MediaType.APPLICATION_JSON_TYPE)).build();
    }

    private static Response tooManyRequests() {
        Map<String, Object> body = Map.of("error", "slow_down", "error_description", "too many requests");
        return Response.status(429).header("Retry-After", "60").entity(body).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    private static Response errorPage(OAuthException e) {
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Authorization error</title></head>"
                + "<body style=\"font-family:sans-serif;padding:40px\"><h2>授权请求无效 / Invalid authorization request</h2>"
                + "<p>" + escape(e.getError()) + ": " + escape(e.getMessage()) + "</p></body></html>";
        return Response.status(400).entity(html.getBytes(StandardCharsets.UTF_8))
                .type(MediaType.TEXT_HTML_TYPE.withCharset("UTF-8")).build();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
