package com.cat2bug.admin.service;

import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 判断 Ollama 是否已启用，并在可用时查询本地模型列表（/api/tags）。
 */
@ApplicationScoped
public class OllamaAvailabilityService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String MODULE_LIST_PATH = "/api/tags";

    @ConfigProperty(name = "cat2bug.ai.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "cat2bug.ai.host")
    Optional<String> host;

    @ConfigProperty(name = "cat2bug.ai.default-business-model")
    Optional<String> defaultBusinessModel;

    @ConfigProperty(name = "cat2bug.ai.models")
    Optional<List<String>> models;

    @Inject
    ObjectMapper objectMapper;

    public boolean isAvailable() {
        return enabled && host.map(h -> !StringUtils.isEmpty(h)).orElse(false);
    }

    public Optional<String> host() {
        return host;
    }

    public String normalizeHost() {
        return normalizeHost(host.orElse(""));
    }

    public List<String> configuredModels() {
        return models.orElse(Collections.emptyList());
    }

    public List<String> defaultModelNames() {
        String name = defaultBusinessModel.orElse(null);
        if (StringUtils.isEmpty(name)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(name.trim());
    }

    /**
     * 已下载模型行：name、size、state=COMPLETED。
     */
    public List<Map<String, Object>> listDownloadedModels() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }
        try {
            return fetchDownloadedModels();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * 与 {@link #listDownloadedModels()} 相同，但 Ollama 不可达或返回非 2xx 时抛异常，供调用方区分「没有模型」与「连不上」。
     */
    public List<Map<String, Object>> fetchDownloadedModels() throws Exception {
        String url = normalizeHost(host.orElse("")) + MODULE_LIST_PATH;
        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode modelsNode = root.path("models");
        if (!modelsNode.isArray()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JsonNode item : modelsNode) {
            String name = item.path("name").asText(null);
            if (StringUtils.isEmpty(name)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("size", item.path("size").asLong(0L));
            row.put("state", "COMPLETED");
            rows.add(row);
        }
        return rows;
    }

    private static String normalizeHost(String raw) {
        String base = raw.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
