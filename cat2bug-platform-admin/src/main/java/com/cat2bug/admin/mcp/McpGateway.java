package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.framework.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.runtime.LaunchMode;
import jakarta.annotation.PostConstruct;
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
import java.util.OptionalInt;
import java.util.StringJoiner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 把 MCP 工具调用转发到同进程的网页接口：为（用户，项目）建一个短时内部会话，
 * 让权限注解、数据范围、操作日志与网页走完全相同的路径。会话用完即删。
 *
 * <p>回环是「工作线程 A 阻塞等待 → 嵌套 REST 还要再占工作线程 B」，并且内层请求要再占一条
 * JDBC 连接。并发工具调用占满线程池时会自死锁；占满连接池时，令牌校验和网页接口一起拿不到连接，
 * 变成 HTTP 500。因此用信号量限制在途回环数，同时按线程池和连接池取较小值。
 */
@ApplicationScoped
public class McpGateway {

    private static final Logger LOG = Logger.getLogger(McpGateway.class);

    static final long SESSION_TTL_SECONDS = 120;
    static final Duration TIMEOUT = Duration.ofSeconds(60);
    static final String CHANNEL = "mcp";
    /** Quarkus 未显式配置时的常见默认上限，仅用于推算回环并发。 */
    static final int DEFAULT_THREAD_POOL_MAX = 200;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int httpPort;

    @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081")
    int testPort;

    @ConfigProperty(name = "quarkus.http.root-path", defaultValue = "/")
    String rootPath;

    @ConfigProperty(name = "cat2bug.mcp.loopback-url")
    Optional<String> loopbackUrl;

    /** 显式上限；未配时按 {@link #resolveLoopbackPermits(int, int, OptionalInt)} 推算。 */
    @ConfigProperty(name = "cat2bug.mcp.loopback.max-in-flight")
    OptionalInt loopbackMaxInFlight;

    @ConfigProperty(name = "quarkus.thread-pool.max-threads")
    OptionalInt threadPoolMaxThreads;

    /** 与网页、令牌校验共用。回环在途数不能超过这个池，否则鉴权拿不到连接，整站变 500。 */
    @ConfigProperty(name = "quarkus.datasource.runtime-mysql.jdbc.max-size", defaultValue = "40")
    int jdbcMaxSize;

    @Inject
    TokenService tokenService;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private Semaphore loopbackSlots;
    private int loopbackPermits;

    @PostConstruct
    void initLoopbackLimiter() {
        loopbackPermits = resolveLoopbackPermits(
                threadPoolMaxThreads.orElse(DEFAULT_THREAD_POOL_MAX),
                jdbcMaxSize,
                loopbackMaxInFlight);
        loopbackSlots = new Semaphore(loopbackPermits, true);
        LOG.infof("MCP 回环并发上限 %d（同时受工作线程与 JDBC 连接池约束，避免占满连接导致全站 500）", loopbackPermits);
    }

    /**
     * 在途回环许可数：显式配置优先。
     * 否则取线程约束与连接池约束的较小值。
     * 线程约束 {@code max(4, maxThreads/3)}，给嵌套 REST 留约 2/3 工作线程。
     * 连接池约束最多用一半（至少留 1 个给网页和令牌校验）。
     */
    public static int resolveLoopbackPermits(int maxThreads, int jdbcMaxSize, OptionalInt explicit) {
        if (explicit != null && explicit.isPresent()) {
            int n = explicit.getAsInt();
            if (n < 1) {
                throw new IllegalArgumentException("cat2bug.mcp.loopback.max-in-flight must be >= 1");
            }
            return n;
        }
        int byThreads = Math.max(4, Math.max(1, maxThreads) / 3);
        int pool = Math.max(1, jdbcMaxSize);
        int reserve = Math.max(1, pool / 2);
        int byDb = Math.max(1, pool - reserve);
        return Math.min(byThreads, byDb);
    }

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
        acquireLoopbackSlot();
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
            // sendAsync：把等响应放到 HttpClient 内部线程，避免再占一条 Quarkus worker 做 IO 等待；
            // 调用方 worker 仍会 join，故必须配合上面的信号量预留嵌套 REST 线程。
            HttpResponse<byte[]> response = client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
                    .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            return unwrap(response.statusCode(), response.body());
        } catch (ToolCallException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolCallException("调用被中断");
        } catch (java.util.concurrent.TimeoutException e) {
            throw new ToolCallException("内部调用超时");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof ToolCallException toolEx) {
                throw toolEx;
            }
            LOG.warnf("MCP 转发 %s %s 失败: %s", method, path, cause.getMessage());
            throw new ToolCallException("内部调用失败：" + cause.getMessage());
        } catch (IOException | RuntimeException e) {
            LOG.warnf("MCP 转发 %s %s 失败: %s", method, path, e.getMessage());
            throw new ToolCallException("内部调用失败：" + e.getMessage());
        } finally {
            if (sessionId != null) {
                tokenService.delLoginUser(sessionId);
            }
            releaseLoopbackSlot();
        }
    }

    private void acquireLoopbackSlot() {
        try {
            if (!loopbackSlots.tryAcquire(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                LOG.warnf("MCP 回环并发已满（上限 %d），拒绝新的内部转发", loopbackPermits);
                throw new ToolCallException("系统繁忙：MCP 内部转发并发已满，请稍后重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolCallException("调用被中断");
        }
    }

    private void releaseLoopbackSlot() {
        if (loopbackSlots != null) {
            loopbackSlots.release();
        }
    }

    /** 测试与运维观测：当前配置的在途回环上限。 */
    int loopbackPermits() {
        return loopbackPermits;
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
