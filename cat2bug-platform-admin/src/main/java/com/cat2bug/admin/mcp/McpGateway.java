package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.framework.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * 把 MCP 工具调用转发到本进程的网页接口：为（用户，项目）建一个短时内部会话，
 * 让权限注解、数据范围、操作日志与网页走完全相同的路径。会话用完即删。
 */
@ApplicationScoped
public class McpGateway {

    private static final Logger LOG = Logger.getLogger(McpGateway.class);

    static final long SESSION_TTL_SECONDS = 120;
    static final Duration TIMEOUT = Duration.ofSeconds(60);
    static final String CHANNEL = "mcp";

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int httpPort;

    @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081")
    int testPort;

    @ConfigProperty(name = "quarkus.http.root-path", defaultValue = "/")
    String rootPath;

    @ConfigProperty(name = "cat2bug.mcp.loopback-url")
    Optional<String> loopbackUrl;

    @Inject
    TokenService tokenService;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public JsonNode get(McpScope scope, String path, Map<String, ?> query) {
        return call(scope, "GET", path, query, null);
    }

    public JsonNode post(McpScope scope, String path, Object body) {
        return call(scope, "POST", path, null, body);
    }

    public JsonNode put(McpScope scope, String path, Object body) {
        return call(scope, "PUT", path, null, body);
    }

    /**
     * @return 网页接口的 JSON 响应体（AjaxResult / TableDataInfo 原样）
     * @throws ToolCallException 非 2xx 或 {@code code != 200}
     */
    public JsonNode call(McpScope scope, String method, String path, Map<String, ?> query, Object body) {
        String sessionId = null;
        try {
            LoginUser session = sessionFor(scope);
            String jwt = tokenService.createInternalToken(session, SESSION_TTL_SECONDS);
            sessionId = session.getToken();
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path + queryString(query)))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + jwt)
                    .header("Accept", "application/json")
                    .header("Accept-Language", "zh-CN")
                    .header("X-Forwarded-For", scope.clientIp() == null ? "127.0.0.1" : scope.clientIp());
            if (body != null) {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(body)));
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                builder.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString("{}"));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return unwrap(response.statusCode(), response.body());
        } catch (ToolCallException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolCallException("调用被中断");
        } catch (IOException | RuntimeException e) {
            LOG.warnf("MCP 转发 %s %s 失败: %s", method, path, e.getMessage());
            throw new ToolCallException("内部调用失败：" + e.getMessage());
        } finally {
            if (sessionId != null) {
                tokenService.delLoginUser(sessionId);
            }
        }
    }

    JsonNode unwrap(int status, byte[] bytes) throws IOException {
        JsonNode node = bytes == null || bytes.length == 0 ? null : objectMapper.readTree(bytes);
        String msg = node != null && node.hasNonNull("msg") ? node.get("msg").asText() : null;
        if (status < 200 || status >= 300) {
            throw new ToolCallException(errorText(status, msg));
        }
        if (node != null && node.has("code") && node.get("code").isNumber() && node.get("code").asInt() != 200) {
            throw new ToolCallException(errorText(node.get("code").asInt(), msg));
        }
        return node;
    }

    private static String errorText(int code, String msg) {
        String reason = switch (code) {
            case 401 -> "未认证";
            case 403 -> "无权限";
            case 404 -> "不存在";
            case 409 -> "状态冲突";
            default -> "失败";
        };
        return msg == null || msg.isBlank() ? reason + "（" + code + "）" : msg;
    }

    LoginUser sessionFor(McpScope scope) {
        LoginUser caller = scope.caller();
        SysUser user = objectMapper.convertValue(McpContext.userOf(caller), SysUser.class);
        user.setRoles(scope.access().roles());
        LoginUser session = new LoginUser();
        session.setUserId(caller.getUserId());
        session.setDeptId(caller.getDeptId());
        session.setUser(user);
        session.setPermissions(new java.util.HashSet<>(scope.access().sessionPermissions()));
        session.setContextPermissionsMerged(true);
        session.setIpaddr(scope.clientIp());
        session.setOperChannel(CHANNEL);
        session.setClientName(scope.clientName());
        session.setMcpProjectId(scope.projectId());
        return session;
    }

    String baseUrl() {
        if (loopbackUrl.isPresent() && !loopbackUrl.get().isBlank()) {
            return trimSlash(loopbackUrl.get().trim());
        }
        int port = LaunchMode.current() == LaunchMode.TEST ? testPort : httpPort;
        String root = rootPath == null ? "" : trimSlash(rootPath.trim());
        if (!root.isEmpty() && !root.startsWith("/")) {
            root = "/" + root;
        }
        return "http://127.0.0.1:" + port + root;
    }

    static String queryString(Map<String, ?> query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&", "?", "");
        query.forEach((k, v) -> {
            if (v == null) {
                return;
            }
            if (v instanceof Collection<?> values) {
                for (Object item : values) {
                    if (item != null) {
                        joiner.add(encode(k) + "=" + encode(String.valueOf(item)));
                    }
                }
            } else {
                joiner.add(encode(k) + "=" + encode(String.valueOf(v)));
            }
        });
        String out = joiner.toString();
        return "?".equals(out) ? "" : out;
    }

    static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String trimSlash(String s) {
        String v = s;
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
