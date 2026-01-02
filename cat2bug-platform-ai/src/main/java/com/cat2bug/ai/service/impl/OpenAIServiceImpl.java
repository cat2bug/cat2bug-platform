package com.cat2bug.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.cat2bug.ai.domain.AiAccount;
import com.cat2bug.ai.service.IAiAccountService;
import com.cat2bug.ai.service.IAiService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-12-29 15:49
 * @Version: 1.0.0
 */
@Service("openai")
@ConditionalOnProperty(prefix = "cat2bug.open-ai", name = "enabled", havingValue = "true")
@ConfigurationProperties(prefix = "cat2bug.open-ai")
public class OpenAIServiceImpl implements IAiService {
    @Autowired
    IAiAccountService aiAccountService;
    @Override
    public <T> T generate(String aiAccountId, String prompt, boolean stream, long[] context, Class<T> cls) {
        AiAccount aiAccount = aiAccountService.selectAiAccountByAccountId(Long.valueOf(aiAccountId));
        if(aiAccount==null) {
            throw new RuntimeException(String.format("未找到aiAccountId:%s 的模型信息", aiAccountId));
        }
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(aiAccount.getAiUrl())
                .apiKey(aiAccount.getApiKey())
                .build();

        StructuredChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model(aiAccount.getModelName())
                .responseFormat(cls)
                .build();

        List<StructuredChatCompletion.Choice<T>> choices = client.chat().completions().create(params).choices();
        for (StructuredChatCompletion.Choice<T> choice : choices) {
            Optional<T> contents = choice.message().content(); // List<BookList>

            String json = JSON.toJSONString(contents.get());
            System.out.println("================"+json);
        }

        return null;
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
