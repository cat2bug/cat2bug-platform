package com.cat2bug.framework.web;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * 平台对外访问地址（OAuth issuer、回调与元数据中的绝对地址）。
 * 优先 {@code cat2bug.public-url}；未配置时按 {@code X-Forwarded-Proto / X-Forwarded-Host / Host} 推导，
 * 反向代理未透传这些头时推导结果会是内网地址，所以生产环境应配置。
 */
@ApplicationScoped
public class PublicUrlResolver {

    private static final Logger LOG = Logger.getLogger(PublicUrlResolver.class);

    @ConfigProperty(name = "cat2bug.public-url")
    Optional<String> publicUrl;

    void onStart(@Observes StartupEvent event) {
        if (!isConfigured()) {
            LOG.info("cat2bug.public-url 未配置：OAuth issuer 与 MCP 地址将按请求头推导，反向代理部署请配置该项");
        }
    }

    public boolean isConfigured() {
        return configured() != null;
    }

    public String resolve(HttpServerRequest request) {
        String configured = configured();
        if (configured != null) {
            return configured;
        }
        if (request == null) {
            return "http://127.0.0.1";
        }
        return derive(request.getHeader("X-Forwarded-Proto"), request.getHeader("X-Forwarded-Host"),
                request.getHeader("Host"), request.scheme());
    }

    public static String derive(String forwardedProto, String forwardedHost, String host, String scheme) {
        String proto = firstValue(forwardedProto);
        if (proto == null) {
            proto = scheme == null || scheme.isBlank() ? "http" : scheme;
        }
        String effectiveHost = firstValue(forwardedHost);
        if (effectiveHost == null) {
            effectiveHost = firstValue(host);
        }
        if (effectiveHost == null) {
            effectiveHost = "127.0.0.1";
        }
        return proto.toLowerCase() + "://" + effectiveHost;
    }

    private String configured() {
        if (publicUrl == null || publicUrl.isEmpty()) {
            return null;
        }
        String value = publicUrl.get().trim();
        if (value.isEmpty()) {
            return null;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String firstValue(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        int comma = header.indexOf(',');
        String value = (comma >= 0 ? header.substring(0, comma) : header).trim();
        return value.isEmpty() ? null : value;
    }
}
