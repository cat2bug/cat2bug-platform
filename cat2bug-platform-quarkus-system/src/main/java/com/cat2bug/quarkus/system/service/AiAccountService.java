package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.AiAccount;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.mapper.AiAccountMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class AiAccountService {

    private static final MediaType JSON_UTF8 = MediaType.parse("application/json;charset=utf-8");
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

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TEST_HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(TEST_HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(TEST_HTTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .build();
        RequestBody body = RequestBody.create(JSON_UTF8, payload);
        Request request = new Request.Builder()
                .url(account.getAiUrl().trim())
                .post(body)
                .addHeader("Authorization", "Bearer " + account.getApiKey().trim())
                .build();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody rb = response.body();
            String responseBody = rb != null ? rb.string() : "";
            if (!response.isSuccessful()) {
                throw new ApiException("请求失败 HTTP " + response.code() + "：" + summarizeErrorBody(responseBody));
            }
            JsonNode parsed = MAPPER.readTree(responseBody);
            JsonNode choices = parsed.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new ApiException("接口返回异常：无模型输出");
            }
            return "OpenAI 接口连接正常";
        } catch (IOException e) {
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
