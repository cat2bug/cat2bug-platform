package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.AiInferenceModelResolver;
import com.cat2bug.quarkus.admin.service.OpenAiGenerateService;
import com.cat2bug.quarkus.admin.service.OllamaGenerateService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.vo.ai.AiCaseList;
import com.cat2bug.quarkus.common.domain.vo.ai.AiSysPrompt;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ai/case")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AiCaseResource {

    private static final String SERVICE_OPENAI = AiInferenceModelResolver.SERVICE_OPENAI;
    private static final String SERVICE_OLLAMA = AiInferenceModelResolver.SERVICE_OLLAMA;

    @Inject
    OllamaGenerateService ollamaGenerateService;

    @Inject
    OpenAiGenerateService openAiGenerateService;

    @Inject
    AiInferenceModelResolver aiInferenceModelResolver;

    @POST
    @Path("/list")
    @RequiresPermissions("system:case:add")
    public AjaxResult list(AiSysPrompt prompt) throws Exception {
        if (prompt == null || StringUtils.isEmpty(prompt.getServiceType())) {
            return AjaxResult.error("AI 服务类型不能为空");
        }
        String json = String.format("%s。前面如果没有标明数量，需要生成%d条测试用例",
                prompt.getPrompt(), Math.max(prompt.getRowCount(), 1));
        String serviceType = aiInferenceModelResolver.normalizeServiceType(prompt.getServiceType());
        if (SERVICE_OPENAI.equals(serviceType)) {
            if (!openAiGenerateService.isAvailable()) {
                return AjaxResult.error("当前环境未启用对应的 AI 服务：" + SERVICE_OPENAI);
            }
            String accountId = aiInferenceModelResolver.resolveOpenAiAccountId(
                    prompt.getModelId(), prompt.getProjectId());
            if (StringUtils.isBlank(accountId)) {
                return AjaxResult.error("未找到可用的 AI 模型，请先配置 OpenAI 账号。");
            }
            AiCaseList cases = openAiGenerateService.generate(
                    accountId, json, false, prompt.getContext(), AiCaseList.class);
            return AjaxResult.success(cases);
        }
        if (SERVICE_OLLAMA.equals(serviceType)) {
            if (!ollamaGenerateService.isAvailable()) {
                return AjaxResult.error("当前环境未启用对应的 AI 服务：" + SERVICE_OLLAMA);
            }
            String ollamaModel = aiInferenceModelResolver.resolveOllamaModelName(prompt.getModelId());
            if (StringUtils.isBlank(ollamaModel)) {
                return AjaxResult.error("未找到可用的 AI 模型，请先配置 Ollama 模型。");
            }
            AiCaseList cases = ollamaGenerateService.generate(
                    ollamaModel, json, false, prompt.getContext(), AiCaseList.class);
            return AjaxResult.success(cases);
        }
        return AjaxResult.error("不支持的 AI 服务类型：" + prompt.getServiceType());
    }
}
