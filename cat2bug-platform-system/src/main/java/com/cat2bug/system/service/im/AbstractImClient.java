package com.cat2bug.system.service.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import jakarta.inject.Inject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * IM 平台 HTTP 客户端公共部分：JSON 请求、token 缓存与「token 失效刷新后重试一次」。
 */
public abstract class AbstractImClient {

    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json;charset=utf-8");

    @Inject
    ImTokenCache tokenCache;

    private volatile OkHttpClient httpClient;

    @FunctionalInterface
    interface TokenCall<T> {
        T call(String token) throws Exception;
    }

    record HttpResult(int status, String body) {
        boolean successful() {
            return status >= 200 && status < 300;
        }

        JsonNode json() {
            if (body == null || body.isBlank()) {
                return MissingNode.getInstance();
            }
            try {
                return MAPPER.readTree(body);
            } catch (IOException e) {
                return MissingNode.getInstance();
            }
        }
    }

    protected <T> T withToken(String platform, String appId, ImTokenCache.TokenLoader loader,
                              TokenCall<T> call) throws Exception {
        String token = tokenCache.get(platform, appId, loader);
        try {
            return call.call(token);
        } catch (ImPushException.TokenInvalid e) {
            tokenCache.invalidate(platform, appId);
            return call.call(tokenCache.get(platform, appId, loader));
        }
    }

    protected HttpResult postJson(String url, Map<String, String> headers, Object body) throws IOException {
        String json = body instanceof String s ? s : MAPPER.writeValueAsString(body);
        Request.Builder builder = new Request.Builder().url(url).post(RequestBody.create(json, JSON_TYPE));
        if (headers != null) {
            headers.forEach(builder::header);
        }
        return execute(builder.build());
    }

    protected HttpResult get(String url) throws IOException {
        return execute(new Request.Builder().url(url).get().build());
    }

    private HttpResult execute(Request request) throws IOException {
        try (Response response = client().newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            return new HttpResult(response.code(), body);
        }
    }

    private OkHttpClient client() {
        OkHttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build();
                    httpClient = client;
                }
            }
        }
        return client;
    }

    static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** 日志与异常里只保留手机号前 3 位和后 2 位。 */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return "***";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 2);
    }

    static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 300 ? body.substring(0, 300) + "..." : body;
    }
}
