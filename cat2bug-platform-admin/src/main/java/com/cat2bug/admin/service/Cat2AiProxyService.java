package com.cat2bug.admin.service;

import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Cat2AI 反向代理，对齐 Spring {@code Cat2AiProxyController} 与 {@code RouterConfig}。
 */
@ApplicationScoped
public class Cat2AiProxyService {

    private static final Logger LOG = Logger.getLogger(Cat2AiProxyService.class);
    private static final String DEFAULT_PREFIX = "cat2ai";

    @ConfigProperty(name = "cat2bug.proxy.routes.ai.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "cat2bug.proxy.routes.ai.host")
    java.util.Optional<String> host;

    @ConfigProperty(name = "cat2bug.proxy.routes.ai.prefix", defaultValue = DEFAULT_PREFIX)
    String prefix;

    @ConfigProperty(name = "cat2bug.proxy.routes.ai.timeout", defaultValue = "600")
    int timeoutSeconds;

    public Response forward(String method, UriInfo uriInfo, String body) {
        if (!enabled) {
            throw new ApiException(404, "Cat2AI proxy is disabled");
        }
        String targetHost = host.filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new ApiException(500, "No route found!"));
        String targetUrl = buildTargetUrl(uriInfo, targetHost, prefix);
        String requestBody = body == null ? "" : body;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header("Connection", "keep-alive")
                    .method(method, HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> upstream = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseText = upstream.body() != null ? upstream.body() : "";
            int status = upstream.statusCode();
            if (status < 200 || status >= 300) {
                LOG.warnf("Cat2AI proxy upstream HTTP %d for %s", status, targetUrl);
            }
            return Response.status(Response.Status.OK)
                    .entity(responseText)
                    .type("application/json")
                    .build();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.errorf(e, "Cat2AI proxy failed for %s", targetUrl);
            return Response.status(Response.Status.OK)
                    .entity("Error " + e.getMessage())
                    .type("application/json")
                    .build();
        }
    }

    static String buildTargetUrl(UriInfo uriInfo, String hostValue, String routePrefix) {
        URI requestUri = uriInfo.getRequestUri();
        String path = requestUri.getPath();
        String normalizedPrefix = "/" + routePrefix.replaceAll("^/+", "").replaceAll("/+$", "");
        String targetPath = path.replace(normalizedPrefix, "");
        String base = hostValue.endsWith("/") ? hostValue.substring(0, hostValue.length() - 1) : hostValue;
        String query = requestUri.getQuery();
        return base + targetPath + (query != null ? "?" + query : "");
    }
}
