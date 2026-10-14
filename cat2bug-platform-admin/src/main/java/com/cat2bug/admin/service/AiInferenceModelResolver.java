package com.cat2bug.admin.service;

import com.cat2bug.common.domain.AiAccount;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.AiAccountService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * 解析缺陷/用例 AI 推理使用的 Ollama 模型名或 OpenAI 账号 ID（对齐 Spring {@code AiInferenceModelResolver}）。
 */
@ApplicationScoped
public class AiInferenceModelResolver {

    public static final String SERVICE_OLLAMA = "ollama";
    public static final String SERVICE_OPENAI = "openai";

    @Inject
    OllamaAvailabilityService ollamaAvailability;

    @Inject
    AiAccountService aiAccountService;

    @Inject
    OpenAiGenerateService openAiGenerateService;

    public String normalizeServiceType(String serviceType) {
        if (StringUtils.isEmpty(serviceType)) {
            return SERVICE_OLLAMA;
        }
        return serviceType;
    }

    /**
     * 已下载 Ollama 模型中第一个名称；若请求已带 modelId 则原样返回。
     */
    public String resolveOllamaModelName(String modelId) {
        if (StringUtils.isNotBlank(modelId)) {
            return modelId;
        }
        if (!ollamaAvailability.isAvailable()) {
            return null;
        }
        List<Map<String, Object>> models = ollamaAvailability.listDownloadedModels();
        for (Map<String, Object> row : models) {
            String name = row == null ? null : (String) row.get("name");
            if (StringUtils.isNotBlank(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * OpenAI 分支：modelId 为账号主键字符串；未传时取该项目下第一个账号。
     */
    public String resolveOpenAiAccountId(String modelId, Long projectId) {
        if (StringUtils.isNotBlank(modelId)) {
            return modelId;
        }
        if (projectId == null) {
            return null;
        }
        AiAccount query = new AiAccount();
        query.setProjectId(projectId);
        List<AiAccount> rows = aiAccountService.selectAiAccountList(query);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return String.valueOf(rows.get(0).getAccountId());
    }

    public boolean isOllamaAvailable() {
        return ollamaAvailability.isAvailable();
    }

    public boolean isOpenAiAvailable() {
        return openAiGenerateService.isAvailable();
    }
}
