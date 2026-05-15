package com.cat2bug.ai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.cat2bug.ai.domain.AiAccount;
import com.cat2bug.ai.mapper.AiAccountMapper;
import com.cat2bug.ai.service.IAiAccountService;
import com.cat2bug.ai.vo.OpenAIRequest;
import com.cat2bug.ai.vo.OpenAIResponse;
import com.cat2bug.common.exception.ServiceException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI账号Service业务层处理
 * 
 * @author yuzhantao
 * @date 2025-12-28
 */
@Service
public class AiAccountServiceImpl implements IAiAccountService 
{
    private static final MediaType JSON_UTF8 = MediaType.parse("application/json;charset=utf-8");

    private static final int TEST_HTTP_TIMEOUT_SEC = 45;

    @Autowired
    private AiAccountMapper aiAccountMapper;

    /**
     * 查询OpenAI账号
     * 
     * @param accountId OpenAI账号主键
     * @return OpenAI账号
     */
    @Override
    public AiAccount selectAiAccountByAccountId(Long accountId)
    {
        return aiAccountMapper.selectAiAccountByAccountId(accountId);
    }

    /**
     * 查询OpenAI账号列表
     * 
     * @param aiAccount OpenAI账号
     * @return OpenAI账号
     */
    @Override
    public List<AiAccount> selectAiAccountList(AiAccount aiAccount)
    {
        return aiAccountMapper.selectAiAccountList(aiAccount);
    }

    /**
     * 新增OpenAI账号
     * 
     * @param aiAccount OpenAI账号
     * @return 结果
     */
    @Override
    public int insertAiAccount(AiAccount aiAccount)
    {
        return aiAccountMapper.insertAiAccount(aiAccount);
    }

    /**
     * 修改OpenAI账号
     * 
     * @param aiAccount OpenAI账号
     * @return 结果
     */
    @Override
    public int updateAiAccount(AiAccount aiAccount)
    {
        return aiAccountMapper.updateAiAccount(aiAccount);
    }

    /**
     * 批量删除OpenAI账号
     * 
     * @param accountIds 需要删除的OpenAI账号主键
     * @return 结果
     */
    @Override
    public int deleteAiAccountByAccountIds(Long[] accountIds)
    {
        return aiAccountMapper.deleteAiAccountByAccountIds(accountIds);
    }

    /**
     * 删除OpenAI账号信息
     * 
     * @param accountId OpenAI账号主键
     * @return 结果
     */
    @Override
    public int deleteAiAccountByAccountId(Long accountId)
    {
        return aiAccountMapper.deleteAiAccountByAccountId(accountId);
    }

    @Override
    public String testOpenAiConnection(Long accountId)
    {
        AiAccount account = aiAccountMapper.selectAiAccountByAccountId(accountId);
        if (account == null)
        {
            throw new ServiceException("账号不存在");
        }
        if (StringUtils.isBlank(account.getAiUrl()))
        {
            throw new ServiceException("AI服务地址为空");
        }
        if (StringUtils.isBlank(account.getApiKey()))
        {
            throw new ServiceException("密钥为空");
        }
        if (StringUtils.isBlank(account.getModelName()))
        {
            throw new ServiceException("模型名称为空");
        }
        List<OpenAIRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAIRequest.Message(OpenAIRequest.ROLE_USER, "ping"));
        String jsonRequest = new OpenAIRequest(account.getModelName(), messages).toString();
        JSONObject root = JSON.parseObject(jsonRequest);
        root.put("max_tokens", 8);
        String payload = root.toJSONString();

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
        try (Response response = client.newCall(request).execute())
        {
            ResponseBody rb = response.body();
            String responseBody = rb != null ? rb.string() : "";
            if (!response.isSuccessful())
            {
                throw new ServiceException("请求失败 HTTP " + response.code() + "：" + summarizeErrorBody(responseBody));
            }
            OpenAIResponse<String> openAIResponse;
            try
            {
                openAIResponse = JSON.parseObject(responseBody,
                        new TypeReference<OpenAIResponse<String>>() {});
            }
            catch (Exception e)
            {
                throw new ServiceException("解析响应失败：" + e.getMessage());
            }
            if (openAIResponse == null || openAIResponse.getChoices() == null || openAIResponse.getChoices().isEmpty())
            {
                throw new ServiceException("接口返回异常：无模型输出");
            }
            return "OpenAI 接口连接正常";
        }
        catch (IOException e)
        {
            throw new ServiceException("网络请求失败：" + e.getMessage());
        }
    }

    private static String summarizeErrorBody(String responseBody)
    {
        if (StringUtils.isBlank(responseBody))
        {
            return "(无响应体)";
        }
        try
        {
            JSONObject json = JSON.parseObject(responseBody);
            if (json.containsKey("error"))
            {
                JSONObject err = json.getJSONObject("error");
                if (err != null && StringUtils.isNotBlank(err.getString("message")))
                {
                    return err.getString("message");
                }
            }
        }
        catch (Exception ignored)
        {
        }
        String trimmed = responseBody.trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "…" : trimmed;
    }
}
