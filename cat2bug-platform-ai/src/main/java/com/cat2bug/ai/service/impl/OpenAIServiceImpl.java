package com.cat2bug.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.cat2bug.ai.domain.AiAccount;
import com.cat2bug.ai.service.IAiAccountService;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.ai.utils.PromptUtils;
import com.cat2bug.ai.vo.AiResponseBody;
import com.cat2bug.ai.vo.OpenAIRequest;
import com.cat2bug.ai.vo.OpenAIResponse;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.utils.uuid.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-12-29 15:49
 * @Version: 1.0.0
 */
@Service("openai")
@ConditionalOnProperty(prefix = "cat2bug.open-ai", name = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "cat2bug.open-ai")
public class OpenAIServiceImpl implements IAiService {
    private final static String OPEN_CONTENT = "openai-content";
    /**
     * 配置项：http超时时间
     */
    private long timeout;

    /**
     * 接口内容类型
     */
    private static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/json;charset=utf-8");
    @Autowired
    IAiAccountService aiAccountService;
    @Autowired
    private RedisCache redisCache;

    private static long currentMessageId = 0;

    @Override
    public <T> T generate(String aiAccountId, String prompt, boolean stream, long[] context, Class<T> cls) throws Exception {
        AiAccount aiAccount = aiAccountService.selectAiAccountByAccountId(Long.valueOf(aiAccountId));
        if(aiAccount==null) {
            throw new RuntimeException(String.format("未找到aiAccountId:%s 的模型信息", aiAccountId));
        }
        // 初始化问答信息
        Map<Long, OpenAIRequest.Message> contentMap = new HashMap<>();
        // 从缓存中获取历史信息
        if(context!=null) {
            for(int i=0;i<context.length;i++) {
                String key = String.valueOf(context[i]);
                if(redisCache.hasKey(OPEN_CONTENT, key)) {
                    contentMap.put(context[i], redisCache.getCacheObject(OPEN_CONTENT, key));
                }
            }
        }
        // 如果没有历史上下文信息，则设置系统信息
        if(contentMap.size()==0) {
            OpenAIRequest.Message sysMessage = new OpenAIRequest.Message(OpenAIRequest.ROLE_SYSTEM,String.format("\n## 输出JSON格式：\n%s\n## 输出的格式要求：1.数据需要压缩成一行\n2.去掉\\n等字符,并且不要有单引号应为双引号", PromptUtils.objectToPrompt(cls)));
            addContent(contentMap, sysMessage);
        }
        // 设置当前问答信息
        OpenAIRequest.Message userMessage = new OpenAIRequest.Message(OpenAIRequest.ROLE_USER,prompt);
        addContent(contentMap, userMessage);
        // 调用OpenAI接口
        OkHttpClient client = new OkHttpClient().newBuilder().readTimeout(this.timeout, TimeUnit.SECONDS).build();
        // 构建请求体：通过 system 消息告诉模型返回结构化 JSON 格式
        String jsonRequest = new OpenAIRequest(
                aiAccount.getModelName(),
                contentMap.values().stream().collect(Collectors.toList())
        ).toString();

        RequestBody body = RequestBody.create(FORM_CONTENT_TYPE, jsonRequest);
        Request request = new Request.Builder()
                .url(aiAccount.getAiUrl())
                .post(body)
                .addHeader("Authorization", "Bearer " + aiAccount.getApiKey())
                .build();

        // 发送请求并获取响应
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            // 解析返回的 JSON 数据
            String responseBody = response.body().string();
            OpenAIResponse<String> openAIResponse = JSON.parseObject(responseBody, new TypeReference<OpenAIResponse<String>>() {});
            // 添加OpenAI反馈信息到上下文
            OpenAIRequest.Message message = new OpenAIRequest.Message(OpenAIRequest.ROLE_ASSISTANT,openAIResponse.getChoices().get(0).getMessage().getContent());
            addContent(contentMap, message);

            T t = JSON.parseObject(openAIResponse.getChoices().get(0).getMessage().getContent(), cls);
            if (AiResponseBody.class.isAssignableFrom(cls)) {
                Field field = AiResponseBody.class.getDeclaredField("context");
                field.setAccessible(true);
                field.set(t, contentMap.keySet().stream().mapToLong(Long::longValue).toArray());
            }

            return t;
        } else {
            throw new RuntimeException(response.message());
        }
    }

    /**
     * 添加OpenAI上下文
     * @param contentMap    上下文Map
     * @param message       消息
     */
    private void addContent(Map<Long, OpenAIRequest.Message> contentMap, OpenAIRequest.Message message) {
        Long messageId = (++OpenAIServiceImpl.currentMessageId)%Long.MAX_VALUE;
        String key = String.valueOf(messageId);
        redisCache.setCacheObject(OPEN_CONTENT, key, message);
        contentMap.put(messageId, message);
    }

    @Override
    public <T> T getModuleInfo(String moduleName, Class<T> cls) {
        return null;
    }

    @Override
    public <T> List<T> getModuleList(Class<T> cls) {
        return new ArrayList<>();
    }

    @Override
    public String pullModule(String moduleName, boolean stream) {
        return "";
    }

    @Override
    public boolean removeModule(String moduleName) {
        return false;
    }
}
