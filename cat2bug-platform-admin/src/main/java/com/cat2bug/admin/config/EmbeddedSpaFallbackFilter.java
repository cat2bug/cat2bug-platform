package com.cat2bug.admin.config;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import jakarta.annotation.Priority;

import com.cat2bug.framework.security.git.GitSmartHttpPathSupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 内嵌前端（classpath:/META-INF/resources/index.html）history 路由：
 * 浏览器 GET 且 Accept 含 text/html 时直接输出 index.html。
 * <p>
 * 避免 GET /login 等 SPA 路径落到仅支持 POST 的 REST 资源上返回 405；
 * REST API（/system/**、/api/** 等）在 Accept 不含 text/html 时不受影响。
 * </p>
 */
@Provider
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class EmbeddedSpaFallbackFilter implements ContainerRequestFilter {

    private static final String INDEX_HTML_LOCATION = "META-INF/resources/index.html";

    /** 与 POST-only REST 同路径的 SPA 页面：GET 一律回退 index.html，避免 405。 */
    private static final Set<String> POST_ONLY_SPA_PATHS = Set.of("/login", "/register");

    /** Namespaced Git Smart HTTP：/{teamKey}/{projectKey}/code/{slug}.git/**（不含 SPA browse 无 .git 路径）。 */
    private static final Pattern NAMESPACED_GIT_SMART_HTTP =
            Pattern.compile("^/[^/]+/[^/]+/code/[^/]+\\.git(?:/.*)?$");

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/static/",
            "/profile/",
            "/docs/",
            "/html/",
            "/q/",
            "/websocket/",
            "/api/",
            "/admin/",
            "/ai/",
            "/cat2ai/",
            "/markdown/",
            "/common/",
            "/git/",
            "/druid/",
            "/captchaImage",
            "/version",
            "/getInfo",
            "/getRouters",
            "/logout",
            "/setup/status",
            "/upgrade/status",
            "/favicon.ico");

    private static final Set<String> EXCLUDED_EXACT = Set.of(
            "/captchaImage",
            "/version",
            "/getInfo",
            "/getRouters",
            "/logout");

    private byte[] indexHtmlBytes;
    private boolean embeddedSpaAvailable;

    @PostConstruct
    void init() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = loader.getResourceAsStream(INDEX_HTML_LOCATION)) {
            if (inputStream == null) {
                embeddedSpaAvailable = false;
                return;
            }
            indexHtmlBytes = inputStream.readAllBytes();
            embeddedSpaAvailable = indexHtmlBytes.length > 0;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + INDEX_HTML_LOCATION, e);
        }
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (!embeddedSpaAvailable || !"GET".equalsIgnoreCase(ctx.getMethod())) {
            return;
        }
        String path = normalizePath(ctx);
        if (isExcluded(path) || (hasStaticExtension(path) && !"/index.html".equals(path))) {
            return;
        }
        if (!shouldServeSpa(ctx, path)) {
            return;
        }
        ctx.abortWith(Response.ok(indexHtmlBytes)
                .type(MediaType.TEXT_HTML_TYPE.withCharset(StandardCharsets.UTF_8.name()))
                .build());
    }

    static boolean shouldServeSpa(ContainerRequestContext ctx, String path) {
        if (POST_ONLY_SPA_PATHS.contains(path)) {
            return true;
        }
        return ctx != null && acceptsHtml(ctx);
    }

    static boolean acceptsHtml(ContainerRequestContext ctx) {
        String accept = ctx.getHeaderString("Accept");
        return accept != null && accept.contains("text/html");
    }

    static String normalizePath(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    static boolean isExcluded(String path) {
        if (EXCLUDED_EXACT.contains(path)) {
            return true;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return GitSmartHttpPathSupport.matchesGitSecurityChain(path)
                || NAMESPACED_GIT_SMART_HTTP.matcher(path).matches();
    }

    static boolean hasStaticExtension(String path) {
        int dot = path.lastIndexOf('.');
        if (dot <= path.lastIndexOf('/')) {
            return false;
        }
        String ext = path.substring(dot + 1).toLowerCase();
        return switch (ext) {
            case "html", "js", "css", "map", "png", "jpg", "jpeg", "gif", "webp", "svg", "ico",
                    "woff", "woff2", "ttf", "eot", "json", "txt", "gz" -> true;
            default -> false;
        };
    }
}
