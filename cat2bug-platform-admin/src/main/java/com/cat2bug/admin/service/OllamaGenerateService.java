package com.cat2bug.admin.service;

import com.cat2bug.admin.util.PromptUtils;
import com.cat2bug.common.domain.vo.ai.AiResponseBody;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ollama /api/generate 推理，对齐 Spring {@code OllamaAiServieImpl#generate}。
 */
@ApplicationScoped
public class OllamaGenerateService {

    private static final Logger LOG = Logger.getLogger(OllamaGenerateService.class);
    private static final String GENERATE_PATH = "/api/generate";
    private static final String PROMPT_FORMAT_TYPE = "json";

    @Inject
    OllamaAvailabilityService ollamaAvailability;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "cat2bug.ai.timeout", defaultValue = "300")
    int timeoutSeconds;

    public boolean isAvailable() {
        return ollamaAvailability.isAvailable();
    }

    public <T> T generate(String moduleName, String prompt, boolean stream, long[] context, Class<T> cls)
            throws Exception {
        if (!isAvailable()) {
            throw new ApiException("Ollama service is not enabled");
        }
        if (StringUtils.isEmpty(moduleName)) {
            throw new ApiException("未找到可用的 AI 模型");
        }
        String requestPrompt;
        String format = null;
        if (cls == String.class) {
            requestPrompt = String.format("请用中文返回,%s", prompt);
        } else {
            requestPrompt = String.format(
                    "请用中文返回,%s,返回JSON格式:\n%s\n返回的JSON数据需要压缩成一行，去掉\\n等字符,并且不要有应为双引号",
                    prompt, PromptUtils.objectToPrompt(cls));
            format = PROMPT_FORMAT_TYPE;
        }

        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("model", moduleName);
        bodyMap.put("prompt", requestPrompt);
        bodyMap.put("stream", stream);
        if (format != null) {
            bodyMap.put("format", format);
        }
        if (context != null) {
            bodyMap.put("context", context);
        }
        String body = objectMapper.writeValueAsString(bodyMap);
        LOG.debugf("Ollama generate request model=%s", moduleName);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 30)))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaAvailability.normalizeHost() + GENERATE_PATH))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiException("AI 推理失败: HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String responseText = textOrNull(root, "response");
        if (responseText == null) {
            return null;
        }
        if (cls == String.class) {
            @SuppressWarnings("unchecked")
            T value = (T) responseText;
            return value;
        }
        T result = objectMapper.readValue(responseText, cls);
        if (result instanceof AiResponseBody aiResponseBody) {
            JsonNode contextNode = root.path("context");
            if (contextNode.isArray()) {
                long[] ctx = new long[contextNode.size()];
                for (int i = 0; i < contextNode.size(); i++) {
                    ctx[i] = contextNode.get(i).asLong();
                }
                aiResponseBody.setContext(ctx);
            }
        }
        return result;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return StringUtils.isEmpty(text) ? null : text;
    }
}
