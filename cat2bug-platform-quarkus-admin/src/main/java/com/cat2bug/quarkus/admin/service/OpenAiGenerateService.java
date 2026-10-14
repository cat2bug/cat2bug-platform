package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.admin.util.PromptUtils;
import com.cat2bug.quarkus.common.domain.AiAccount;
import com.cat2bug.quarkus.common.domain.vo.ai.AiResponseBody;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.cache.AppCache;
import com.cat2bug.quarkus.system.service.AiAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenAI Chat Completions 推理，对齐 Spring {@code OpenAIServiceImpl#generate}。
 */
@ApplicationScoped
public class OpenAiGenerateService {

    private static final Logger LOG = Logger.getLogger(OpenAiGenerateService.class);
    private static final String OPEN_CONTENT_REGION = "openai-content";
    private static final String ROLE_SYSTEM = "system";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private static final AtomicLong MESSAGE_ID_SEQ = new AtomicLong(0);

    @Inject
    AiAccountService aiAccountService;

    @Inject
    AppCache appCache;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "cat2bug.open-ai.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "cat2bug.open-ai.timeout", defaultValue = "300")
    int timeoutSeconds;

    public boolean isAvailable() {
        return enabled;
    }

    public <T> T generate(String aiAccountId, String prompt, boolean stream, long[] context, Class<T> cls)
            throws Exception {
        if (!isAvailable()) {
            throw new ApiException("OpenAI service is not enabled");
        }
        if (StringUtils.isBlank(aiAccountId)) {
            throw new ApiException("未找到可用的 AI 模型");
        }
        AiAccount aiAccount = aiAccountService.selectAiAccountByAccountId(Long.valueOf(aiAccountId));
        if (aiAccount == null) {
            throw new ApiException(String.format("未找到aiAccountId:%s 的模型信息", aiAccountId));
        }

        Map<Long, OpenAiMessage> contentMap = new LinkedHashMap<>();
        if (context != null) {
            for (long id : context) {
                OpenAiMessage cached = appCache.get(OPEN_CONTENT_REGION, String.valueOf(id), OpenAiMessage.class);
                if (cached != null) {
                    contentMap.put(id, cached);
                }
            }
        }
        if (contentMap.isEmpty()) {
            String sysPrompt = String.format(
                    "\n## 输出JSON格式：\n%s\n## 输出的格式要求：1.数据需要压缩成一行\n2.去掉\\n等字符,并且不要有单引号应为双引号",
                    PromptUtils.objectToPrompt(cls));
            addContent(contentMap, new OpenAiMessage(ROLE_SYSTEM, sysPrompt));
        }
        addContent(contentMap, new OpenAiMessage(ROLE_USER, prompt));

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", aiAccount.getModelName());
        ArrayNode messages = root.putArray("messages");
        for (OpenAiMessage message : contentMap.values()) {
            ObjectNode row = messages.addObject();
            row.put("role", message.role);
            row.put("content", message.content);
        }
        String body = objectMapper.writeValueAsString(root);
        LOG.debugf("OpenAI generate request accountId=%s model=%s", aiAccountId, aiAccount.getModelName());

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 30)))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiAccount.getAiUrl().trim()))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Authorization", "Bearer " + aiAccount.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiException("AI 推理失败: HTTP " + response.statusCode());
        }

        JsonNode responseRoot = objectMapper.readTree(response.body());
        JsonNode choices = responseRoot.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new ApiException("接口返回异常：无模型输出");
        }
        String assistantContent = choices.get(0).path("message").path("content").asText(null);
        if (StringUtils.isBlank(assistantContent)) {
            throw new ApiException("接口返回异常：模型输出为空");
        }

        addContent(contentMap, new OpenAiMessage(ROLE_ASSISTANT, assistantContent));

        if (cls == String.class) {
            @SuppressWarnings("unchecked")
            T value = (T) assistantContent;
            return value;
        }
        T result = objectMapper.readValue(assistantContent, cls);
        if (result instanceof AiResponseBody aiResponseBody) {
            long[] ctx = contentMap.keySet().stream().mapToLong(Long::longValue).toArray();
            aiResponseBody.setContext(ctx);
        }
        return result;
    }

    private void addContent(Map<Long, OpenAiMessage> contentMap, OpenAiMessage message) {
        long messageId = MESSAGE_ID_SEQ.incrementAndGet() % Long.MAX_VALUE;
        String key = String.valueOf(messageId);
        appCache.set(OPEN_CONTENT_REGION, key, message);
        contentMap.put(messageId, message);
    }

    public static final class OpenAiMessage {
        public final String role;
        public final String content;

        public OpenAiMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
