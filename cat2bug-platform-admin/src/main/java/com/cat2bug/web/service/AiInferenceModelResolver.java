package com.cat2bug.web.service;

import com.cat2bug.ai.domain.AiAccount;
import com.cat2bug.ai.service.IAiAccountService;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.ai.vo.AiModule;
import com.cat2bug.ai.vo.AiModuleStateEnum;
import com.cat2bug.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 解析缺陷/用例等 AI 推理调用使用的 Ollama 模型名或 OpenAI 账号 ID。
 */
@Component
public class AiInferenceModelResolver {
    public static final String SERVICE_OLLAMA = "ollama";
    public static final String SERVICE_OPENAI = "openai";

    @Autowired(required = false)
    private Map<String, IAiService> aiServiceMap;

    @Autowired(required = false)
    private IAiAccountService aiAccountService;

    public String normalizeServiceType(String serviceType) {
        if (StringUtils.isEmpty(serviceType)) {
            return SERVICE_OLLAMA;
        }
        return serviceType;
    }

    /**
     * 已下载（COMPLETED）的 Ollama 模型中第一个名称；若请求已带 modelId 则原样返回。
     */
    public String resolveOllamaModelName(String modelId) {
        if (StringUtils.isNotBlank(modelId)) {
            return modelId;
        }
        IAiService ollama = aiServiceMap != null ? aiServiceMap.get(SERVICE_OLLAMA) : null;
        if (ollama == null) {
            return null;
        }
        List<AiModule> list = ollama.getModuleList(AiModule.class);
        if (list == null) {
            return null;
        }
        for (AiModule m : list) {
            if (m != null && m.getState() == AiModuleStateEnum.COMPLETED && StringUtils.isNotBlank(m.getName())) {
                return m.getName();
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
        if (aiAccountService == null || projectId == null) {
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
}
