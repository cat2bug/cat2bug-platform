package com.cat2bug.admin.service;

import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Cat2AI 反向代理，对齐 Spring {@code Cat2AiProxyController} 与 {@code RouterConfig}。
 */
@ApplicationScoped
public class Cat2AiProxyService {

    private static final Logger LOG = Logger.getLogger(Cat2AiProxyService.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
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
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                    .build();
            RequestBody okhttpBody = RequestBody.create(requestBody, JSON);
            Request request = new Request.Builder()
                    .url(targetUrl)
                    .method(method, okhttpBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Connection", "keep-alive")
                    .build();
            try (okhttp3.Response upstream = client.newCall(request).execute()) {
                ResponseBody upstreamBody = upstream.body();
                String responseText = upstreamBody != null ? upstreamBody.string() : "";
                if (!upstream.isSuccessful()) {
                    LOG.warnf("Cat2AI proxy upstream HTTP %d for %s", upstream.code(), targetUrl);
                }
                return Response.status(Response.Status.OK)
                        .entity(responseText)
                        .type("application/json")
                        .build();
            }
        } catch (IOException e) {
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
