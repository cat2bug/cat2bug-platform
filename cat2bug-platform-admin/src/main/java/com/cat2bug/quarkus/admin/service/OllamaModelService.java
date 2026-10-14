package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.api.WebSocketResult;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.common.websocket.MessageWebsocketPush;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ollama 模型拉取/删除，对齐 Spring {@code OllamaAiServieImpl}。
 */
@ApplicationScoped
public class OllamaModelService {

    public static final String PULL_MODEL_TOPIC = "aiModelPull";

    private static final Logger LOG = Logger.getLogger(OllamaModelService.class);
    private static final String PULL_PATH = "/api/pull";
    private static final String DELETE_PATH = "/api/delete";
    private static final String REJECTED_DELETE_HOST = "https://www.cat2bug.com:8023";

    @Inject
    OllamaAvailabilityService ollamaAvailability;

    @Inject
    MessageWebsocketPush messageWebsocketPush;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "cat2bug.ai.timeout", defaultValue = "300")
    int timeoutSeconds;

    /**
     * 拉取模型；{@code stream=true} 时异步推送 WebSocket 进度（与 Spring 一致）。
     */
    public String pullModule(String moduleName, boolean stream) {
        requireAvailable();
        if (StringUtils.isEmpty(moduleName)) {
            throw new ApiException("模型名称不能为空");
        }
        String url = apiUrl(PULL_PATH);
        String body = "{\"name\":\"" + escapeJson(moduleName) + "\",\"stream\":" + stream + "}";
        if (stream) {
            Thread thread = new Thread(() -> streamPull(moduleName, url, body), "ollama-pull-" + moduleName);
            thread.setDaemon(true);
            thread.start();
            return null;
        }
        return syncPull(url, body);
    }

    public boolean removeModule(String moduleName) {
        requireAvailable();
        if (StringUtils.isEmpty(moduleName)) {
            throw new ApiException("模型名称不能为空");
        }
        String host = ollamaAvailability.host().orElse("").trim();
        if (REJECTED_DELETE_HOST.equalsIgnoreCase(host)) {
            throw new ApiException("当前 AI 服务器不允许删除模型");
        }
        String url = apiUrl(DELETE_PATH);
        String body = "{\"name\":\"" + escapeJson(moduleName) + "\"}";
        try {
            HttpClient client = httpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            LOG.debugf(e, "Ollama 删除模型失败 name=%s", moduleName);
            return false;
        }
    }

    private void streamPull(String moduleName, String url, String body) {
        try {
            HttpClient client = httpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                pushPullEvent(moduleName, "ERROR", null, null, 0, 0,
                        "拉取模型失败: HTTP " + response.statusCode());
                return;
            }
            try (BufferedReader reader = new BufferedReader(new StringReader(response.body()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (StringUtils.isEmpty(line)) {
                        continue;
                    }
                    JsonNode node = objectMapper.readTree(line);
                    String error = textOrNull(node, "error");
                    if (StringUtils.isNotEmpty(error)) {
                        if ("pull model manifest: file does not exist".equals(error)) {
                            error = "模型文件不存在: " + moduleName;
                        }
                        pushPullEvent(moduleName, "ERROR", null, null,
                                node.path("total").asLong(0),
                                node.path("completed").asLong(0),
                                error);
                        continue;
                    }
                    String status = textOrNull(node, "status");
                    if (StringUtils.isEmpty(status)) {
                        continue;
                    }
                    if (status.startsWith("pulling")) {
                        String layer = status.length() > 8 ? status.substring(8) : status;
                        pushPullEvent(moduleName, "PULLING", layer, null,
                                node.path("total").asLong(0),
                                node.path("completed").asLong(0),
                                null);
                    } else if (status.startsWith("success")) {
                        pushPullEvent(moduleName, "SUCCESS", null, null,
                                node.path("total").asLong(0),
                                node.path("completed").asLong(0),
                                null);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debugf(e, "Ollama 流式拉取失败 name=%s", moduleName);
            pushPullEvent(moduleName, "ERROR", null, null, 0, 0, e.getMessage());
        }
    }

    private String syncPull(String url, String body) {
        try {
            HttpClient client = httpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException("拉取模型失败: HTTP " + response.statusCode());
            }
            JsonNode node = objectMapper.readTree(response.body());
            return textOrNull(node, "status");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("拉取模型失败: " + e.getMessage());
        }
    }

    private void pushPullEvent(String name, String state, String layer, String ignored,
                               long total, long completed, String error) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("state", state);
        payload.put("total", total);
        payload.put("completed", completed);
        if (layer != null) {
            payload.put("layer", layer);
        }
        if (error != null) {
            payload.put("error", error);
        }
        messageWebsocketPush.sendToAll(WebSocketResult.success(PULL_MODEL_TOPIC, payload));
    }

    private void requireAvailable() {
        if (!ollamaAvailability.isAvailable()) {
            throw new ApiException("Ollama service is not enabled");
        }
    }

    private String apiUrl(String path) {
        return ollamaAvailability.normalizeHost() + path;
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 30)))
                .build();
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return StringUtils.isEmpty(text) ? null : text;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
