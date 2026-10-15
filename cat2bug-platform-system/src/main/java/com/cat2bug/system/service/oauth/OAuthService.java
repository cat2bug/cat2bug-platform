package com.cat2bug.system.service.oauth;

import com.cat2bug.common.domain.SysOAuthClient;
import com.cat2bug.common.domain.SysOAuthCode;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.system.mapper.SysOAuthClientMapper;
import com.cat2bug.system.mapper.SysOAuthCodeMapper;
import com.cat2bug.system.mapper.SysUserAccessTokenMapper;
import com.cat2bug.system.service.SysUserAccessTokenService;
import com.cat2bug.system.service.mcp.McpAccessService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 面向 MCP 客户端的最小 OAuth 2.1 授权服务：动态客户端注册（RFC 7591）、授权码 + PKCE S256（RFC 7636）、
 * 刷新令牌轮换与重放检测、吊销（RFC 7009）。令牌与授权码只存 SHA-256。
 */
@ApplicationScoped
public class OAuthService {

    public static final long CODE_TTL_MILLIS = 10 * 60_000L;
    public static final long ACCESS_TTL_SECONDS = 3600L;
    public static final long REFRESH_TTL_MILLIS = 30L * 86_400_000L;
    public static final String SCOPE = "mcp";

    static final int MAX_REDIRECT_URIS = 10;
    static final int MAX_CLIENT_NAME = 255;
    private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost", "[::1]", "::1");
    private static final Pattern VERIFIER = Pattern.compile("[A-Za-z0-9\\-._~]{43,128}");

    @Inject
    SysOAuthClientMapper clientMapper;

    @Inject
    SysOAuthCodeMapper codeMapper;

    @Inject
    SysUserAccessTokenMapper tokenMapper;

    @Inject
    SysUserAccessTokenService tokenService;

    @Inject
    McpAccessService mcpAccessService;

    public record AuthorizeRequest(String responseType, String clientId, String redirectUri, String codeChallenge,
                                   String codeChallengeMethod, String state, String resource) {
    }

    public record ValidatedAuthorize(SysOAuthClient client, String redirectUri, AuthorizeRequest request) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, String scope) {
    }

    // ---------- 注册 ----------

    public SysOAuthClient register(String clientName, List<String> redirectUris) {
        if (redirectUris == null || redirectUris.isEmpty() || redirectUris.size() > MAX_REDIRECT_URIS) {
            throw new OAuthException("invalid_redirect_uri", "redirect_uris is required (1-" + MAX_REDIRECT_URIS + ")");
        }
        for (String uri : redirectUris) {
            if (!isAllowedRedirectUri(uri)) {
                throw new OAuthException("invalid_redirect_uri",
                        "redirect_uri must be https or an http loopback address: " + uri);
            }
        }
        String name = clientName == null || clientName.isBlank() ? "MCP Client" : clientName.trim();
        if (name.length() > MAX_CLIENT_NAME) {
            name = name.substring(0, MAX_CLIENT_NAME);
        }
        SysOAuthClient client = new SysOAuthClient();
        client.setClientId("c2b_client_" + UUID.randomUUID().toString().replace("-", ""));
        client.setClientName(name);
        client.setRedirectUris(String.join("\n", redirectUris));
        client.setCreateTime(new Date());
        clientMapper.insert(client);
        return client;
    }

    public static boolean isAllowedRedirectUri(String value) {
        if (value == null || value.isBlank() || value.contains("\n")) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            return false;
        }
        if (uri.getFragment() != null || uri.getHost() == null || uri.getUserInfo() != null) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if ("https".equals(scheme)) {
            return true;
        }
        return "http".equals(scheme) && LOOPBACK_HOSTS.contains(uri.getHost().toLowerCase());
    }

    // ---------- 授权 ----------

    public ValidatedAuthorize validate(AuthorizeRequest request) {
        SysOAuthClient client = request.clientId() == null ? null : clientMapper.selectById(request.clientId());
        if (client == null) {
            throw OAuthException.notRedirectable("invalid_client", "unknown client_id");
        }
        List<String> registered = client.redirectUriList();
        String redirectUri = request.redirectUri();
        if (redirectUri == null || redirectUri.isBlank()) {
            if (registered.size() != 1) {
                throw OAuthException.notRedirectable("invalid_request", "redirect_uri is required");
            }
            redirectUri = registered.get(0);
        } else if (!registered.contains(redirectUri)) {
            throw OAuthException.notRedirectable("invalid_request", "redirect_uri does not match a registered value");
        }
        if (!"code".equals(request.responseType())) {
            throw OAuthException.redirectTo(redirectUri, "unsupported_response_type", "only response_type=code is supported");
        }
        if (request.codeChallenge() == null || request.codeChallenge().isBlank()) {
            throw OAuthException.redirectTo(redirectUri, "invalid_request", "code_challenge is required");
        }
        if (!"S256".equals(request.codeChallengeMethod())) {
            throw OAuthException.redirectTo(redirectUri, "invalid_request", "code_challenge_method must be S256");
        }
        return new ValidatedAuthorize(client, redirectUri, request);
    }

    /** 确认页可选项目：本人是成员且已开启 MCP */
    public List<SysProject> selectableProjects(Long userId) {
        return mcpAccessService.enabledProjectsOf(userId);
    }

    /** 用户同意：校验所选项目后签发一次性授权码，返回带 code 与 state 的回调地址 */
    public String approve(Long userId, Long projectId, AuthorizeRequest request) {
        ValidatedAuthorize validated = validate(request);
        if (!isSelectable(userId, projectId)) {
            throw OAuthException.notRedirectable("invalid_request", "project is not selectable for MCP");
        }
        String code = AccessTokens.generate(AccessTokens.OAUTH_CODE_PREFIX);
        Date now = new Date();
        SysOAuthCode row = new SysOAuthCode();
        row.setCodeHash(AccessTokens.sha256Hex(code));
        row.setClientId(validated.client().getClientId());
        row.setUserId(userId);
        row.setProjectId(projectId);
        row.setRedirectUri(validated.redirectUri());
        row.setCodeChallenge(request.codeChallenge());
        row.setResource(blankToNull(request.resource()));
        row.setExpiresAt(new Date(now.getTime() + CODE_TTL_MILLIS));
        row.setCreateTime(now);
        codeMapper.insert(row);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("code", code);
        params.put("state", request.state());
        return appendQuery(validated.redirectUri(), params);
    }

    boolean isSelectable(Long userId, Long projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        return selectableProjects(userId).stream().anyMatch(p -> projectId.equals(p.getProjectId()));
    }

    public String deny(AuthorizeRequest request) {
        ValidatedAuthorize validated = validate(request);
        return errorRedirect(validated.redirectUri(), "access_denied", "user denied the request", request.state());
    }

    public static String errorRedirect(String redirectUri, String error, String description, String state) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("error", error);
        params.put("error_description", description);
        params.put("state", state);
        return appendQuery(redirectUri, params);
    }

    // ---------- 令牌 ----------

    public TokenResponse exchangeCode(String code, String clientId, String redirectUri, String codeVerifier,
                                      String resource) {
        if (code == null || code.isBlank() || clientId == null || clientId.isBlank()) {
            throw new OAuthException("invalid_request", "code and client_id are required");
        }
        String codeHash = AccessTokens.sha256Hex(code);
        SysOAuthCode row = codeMapper.selectByHash(codeHash);
        if (row == null || codeMapper.markUsed(codeHash) == 0) {
            throw new OAuthException("invalid_grant", "authorization code is invalid or already used");
        }
        if (!row.getExpiresAt().after(new Date())) {
            throw new OAuthException("invalid_grant", "authorization code expired");
        }
        if (!Objects.equals(row.getClientId(), clientId)) {
            throw new OAuthException("invalid_grant", "client_id mismatch");
        }
        if (row.getProjectId() == null) {
            throw new OAuthException("invalid_grant", "authorization code is not bound to a project");
        }
        if (redirectUri != null && !redirectUri.isBlank() && !Objects.equals(row.getRedirectUri(), redirectUri)) {
            throw new OAuthException("invalid_grant", "redirect_uri mismatch");
        }
        if (codeVerifier == null || !VERIFIER.matcher(codeVerifier).matches()
                || !AccessTokens.constantTimeEquals(AccessTokens.pkceChallenge(codeVerifier), row.getCodeChallenge())) {
            throw new OAuthException("invalid_grant", "code_verifier does not match");
        }
        String boundResource = row.getResource();
        if (boundResource != null && resource != null && !resource.isBlank() && !boundResource.equals(resource)) {
            throw new OAuthException("invalid_target", "resource does not match the authorization request");
        }
        SysOAuthClient client = clientMapper.selectById(clientId);
        Date now = new Date();
        clientMapper.touch(clientId, now);
        return issue(row.getUserId(), row.getProjectId(), client, UUID.randomUUID().toString(), boundResource, now);
    }

    public TokenResponse refresh(String refreshToken, String clientId, String resource) {
        if (refreshToken == null || refreshToken.isBlank() || clientId == null || clientId.isBlank()) {
            throw new OAuthException("invalid_request", "refresh_token and client_id are required");
        }
        SysUserAccessToken row = tokenMapper.selectByHash(AccessTokens.sha256Hex(refreshToken));
        if (row == null || !SysUserAccessToken.SOURCE_OAUTH_REFRESH.equals(row.getSource()) || row.getProjectId() == null) {
            throw new OAuthException("invalid_grant", "refresh token is invalid");
        }
        Date now = new Date();
        if (row.getRevokedAt() != null) {
            tokenMapper.revokeGrant(row.getGrantId(), now);
            throw new OAuthException("invalid_grant", "refresh token was already used");
        }
        if (!Objects.equals(row.getClientId(), clientId)) {
            throw new OAuthException("invalid_grant", "client_id mismatch");
        }
        if (row.getExpiresAt() != null && !row.getExpiresAt().after(now)) {
            throw new OAuthException("invalid_grant", "refresh token expired");
        }
        if (resource != null && !resource.isBlank() && row.getResource() != null && !row.getResource().equals(resource)) {
            throw new OAuthException("invalid_target", "resource does not match the authorization");
        }
        if (tokenMapper.revokeById(row.getTokenId(), now) == 0) {
            tokenMapper.revokeGrant(row.getGrantId(), now);
            throw new OAuthException("invalid_grant", "refresh token was already used");
        }
        if (tokenService.activeUser(row.getUserId()) == null) {
            tokenMapper.revokeGrant(row.getGrantId(), now);
            throw new OAuthException("invalid_grant", "user is not active");
        }
        SysOAuthClient client = clientMapper.selectById(clientId);
        clientMapper.touch(clientId, now);
        return issue(row.getUserId(), row.getProjectId(), client, row.getGrantId(), row.getResource(), now);
    }

    /** RFC 7009：未知令牌也视为成功；刷新令牌吊销整条授权 */
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        SysUserAccessToken row = tokenMapper.selectByHash(AccessTokens.sha256Hex(token));
        if (row == null) {
            return;
        }
        Date now = new Date();
        if (SysUserAccessToken.SOURCE_OAUTH_REFRESH.equals(row.getSource()) && row.getGrantId() != null) {
            tokenMapper.revokeGrant(row.getGrantId(), now);
        } else {
            tokenMapper.revokeById(row.getTokenId(), now);
        }
    }

    public void cleanup() {
        Date now = new Date();
        codeMapper.deleteExpiredBefore(new Date(now.getTime() - 86_400_000L));
        tokenMapper.deleteInactiveBefore(new Date(now.getTime() - REFRESH_TTL_MILLIS));
        clientMapper.deleteUnusedBefore(new Date(now.getTime() - REFRESH_TTL_MILLIS), now);
    }

    private TokenResponse issue(Long userId, Long projectId, SysOAuthClient client, String grantId, String resource,
                                Date now) {
        String name = client == null ? "OAuth" : client.getClientName();
        String clientId = client == null ? null : client.getClientId();
        String access = AccessTokens.generate(AccessTokens.OAUTH_ACCESS_PREFIX);
        SysUserAccessToken accessRow = tokenService.newRow(userId, projectId, name, access, SysUserAccessToken.SOURCE_OAUTH,
                new Date(now.getTime() + ACCESS_TTL_SECONDS * 1000L), now);
        accessRow.setClientId(clientId);
        accessRow.setGrantId(grantId);
        accessRow.setResource(resource);
        tokenMapper.insert(accessRow);

        String refresh = AccessTokens.generate(AccessTokens.OAUTH_REFRESH_PREFIX);
        SysUserAccessToken refreshRow = tokenService.newRow(userId, projectId, name, refresh,
                SysUserAccessToken.SOURCE_OAUTH_REFRESH, new Date(now.getTime() + REFRESH_TTL_MILLIS), now);
        refreshRow.setClientId(clientId);
        refreshRow.setGrantId(grantId);
        refreshRow.setResource(resource);
        tokenMapper.insert(refreshRow);
        return new TokenResponse(access, refresh, ACCESS_TTL_SECONDS, SCOPE);
    }

    static String appendQuery(String base, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(base);
        char sep = base.contains("?") ? '&' : '?';
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            sb.append(sep).append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            sep = '&';
        }
        return sb.toString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
