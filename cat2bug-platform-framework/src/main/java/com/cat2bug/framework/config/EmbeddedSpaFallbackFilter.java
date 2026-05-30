package com.cat2bug.framework.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 内嵌前端（classpath:/static/index.html）history 路由：浏览器 GET 且 Accept 含 text/html 时直接输出 index.html。
 * <p>
 * 避免 GET /login 等 SPA 路径落到 {@code @PostMapping("/login")} 等 API 上返回 405；
 * 不使用 {@code forward:/index.html}，以免与 Thymeleaf/Controller 视图解析冲突。
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnResource(resources = "classpath:/static/index.html")
public class EmbeddedSpaFallbackFilter extends OncePerRequestFilter
{
    private static final String INDEX_HTML_LOCATION = "classpath:/static/index.html";

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/static/",
            "/profile/",
            "/docs/",
            "/html/",
            "/swagger",
            "/druid/",
            "/h2/",
            "/websocket/",
            "/webjars/",
            "/v3/api-docs",
            "/doc.html",
            "/favicon.ico",
            "/captchaImage",
            "/version",
            "/setup/status",
            "/upgrade/status",
            "/error");

    private final Resource indexHtml;

    public EmbeddedSpaFallbackFilter(ResourceLoader resourceLoader)
    {
        this.indexHtml = resourceLoader.getResource(INDEX_HTML_LOCATION);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if (!HttpMethod.GET.matches(request.getMethod()) || !acceptsHtml(request))
        {
            chain.doFilter(request, response);
            return;
        }
        String path = normalizePath(request);
        if (isExcluded(path) || (hasStaticExtension(path) && !"/index.html".equals(path)))
        {
            chain.doFilter(request, response);
            return;
        }
        if (!indexHtml.exists())
        {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        try (InputStream inputStream = indexHtml.getInputStream())
        {
            StreamUtils.copy(inputStream, response.getOutputStream());
        }
    }

    static boolean acceptsHtml(HttpServletRequest request)
    {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    static String normalizePath(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
        {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    static boolean isExcluded(String path)
    {
        for (String prefix : EXCLUDED_PREFIXES)
        {
            if (path.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    static boolean hasStaticExtension(String path)
    {
        int dot = path.lastIndexOf('.');
        if (dot <= path.lastIndexOf('/'))
        {
            return false;
        }
        String ext = path.substring(dot + 1).toLowerCase();
        return switch (ext)
        {
            case "html", "js", "css", "map", "png", "jpg", "jpeg", "gif", "webp", "svg", "ico", "woff", "woff2", "ttf", "eot", "json", "txt", "gz" -> true;
            default -> false;
        };
    }
}
