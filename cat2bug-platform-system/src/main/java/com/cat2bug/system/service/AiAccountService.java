package com.cat2bug.system.service;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.core.page.PageDomain;
import com.cat2bug.common.domain.AiAccount;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.AiAccountMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class AiAccountService {

    private static final int TEST_HTTP_TIMEOUT_SEC = 45;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    AiAccountMapper aiAccountMapper;

    public TableDataInfo selectAiAccountPage(AiAccount query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        List<AiAccount> all = aiAccountMapper.selectAiAccountList(query);
        int from = Math.min(page.getOffset(), all.size());
        int to = Math.min(from + page.getPageSize(), all.size());
        return new TableDataInfo(all.subList(from, to), all.size());
    }

    public List<AiAccount> selectAiAccountList(AiAccount query) {
        return aiAccountMapper.selectAiAccountList(query);
    }

    public AiAccount selectAiAccountByAccountId(Long accountId) {
        return aiAccountMapper.selectAiAccountByAccountId(accountId);
    }

    public int insertAiAccount(AiAccount aiAccount) {
        return aiAccountMapper.insertAiAccount(aiAccount);
    }

    public int updateAiAccount(AiAccount aiAccount) {
        return aiAccountMapper.updateAiAccount(aiAccount);
    }

    public int deleteAiAccountByAccountIds(Long[] accountIds) {
        return aiAccountMapper.deleteAiAccountByAccountIds(accountIds);
    }

    public String testOpenAiConnection(Long accountId) {
        AiAccount account = aiAccountMapper.selectAiAccountByAccountId(accountId);
        if (account == null) {
            throw new ApiException("账号不存在");
        }
        if (StringUtils.isBlank(account.getAiUrl())) {
            throw new ApiException("AI服务地址为空");
        }
        if (StringUtils.isBlank(account.getApiKey())) {
            throw new ApiException("密钥为空");
        }
        if (StringUtils.isBlank(account.getModelName())) {
            throw new ApiException("模型名称为空");
        }

        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", account.getModelName());
        root.put("max_tokens", 8);
        ArrayNode messages = root.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", "ping");
        String payload;
        try {
            payload = MAPPER.writeValueAsString(root);
        } catch (IOException e) {
            throw new ApiException("构建请求失败：" + e.getMessage());
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(TEST_HTTP_TIMEOUT_SEC))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(account.getAiUrl().trim()))
                .timeout(Duration.ofSeconds(TEST_HTTP_TIMEOUT_SEC))
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Authorization", "Bearer " + account.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String responseBody = response.body() != null ? response.body() : "";
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new ApiException("请求失败 HTTP " + status + "：" + summarizeErrorBody(responseBody));
            }
            JsonNode parsed = MAPPER.readTree(responseBody);
            JsonNode choices = parsed.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new ApiException("接口返回异常：无模型输出");
            }
            return "OpenAI 接口连接正常";
        } catch (IOException e) {
            throw new ApiException("网络请求失败：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("网络请求失败：" + e.getMessage());
        }
    }

    private static String summarizeErrorBody(String responseBody) {
        if (StringUtils.isBlank(responseBody)) {
            return "(无响应体)";
        }
        try {
            JsonNode json = MAPPER.readTree(responseBody);
            JsonNode err = json.path("error");
            if (err.isObject()) {
                String message = err.path("message").asText(null);
                if (StringUtils.isNotBlank(message)) {
                    return message;
                }
            }
        } catch (Exception ignored) {
        }
        String trimmed = responseBody.trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "…" : trimmed;
    }
}
