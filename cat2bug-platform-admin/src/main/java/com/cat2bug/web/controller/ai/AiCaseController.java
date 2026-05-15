package com.cat2bug.web.controller.ai;

import com.cat2bug.ai.service.IAiService;
import com.cat2bug.ai.utils.PromptUtils;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.web.service.AiInferenceModelResolver;
import com.cat2bug.web.vo.AiCaseList;
import com.cat2bug.web.vo.AiSysPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-15 02:26
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/ai/case")
public class AiCaseController extends BaseController {

    private final static String SERVICE_TYPE_OPEN_ID = AiInferenceModelResolver.SERVICE_OPENAI;
    private final static String SERVICE_TYPE_OLLAMA = AiInferenceModelResolver.SERVICE_OLLAMA;
    @Autowired(required = false)
    private Map<String, IAiService> aiServiceMap;

    @Autowired
    private AiInferenceModelResolver aiInferenceModelResolver;
    /**
     * 查询测试用例列表
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody AiSysPrompt prompt) throws Exception {
        IAiService aiService = this.aiServiceMap.get(prompt.getServiceType());
        String json = String.format("%s。前面如果没有标明数量，需要生成%d条测试用例", prompt.getPrompt(), Math.max(prompt.getRowCount(), 1));
        AiCaseList cases = null;
        switch (prompt.getServiceType()) {
            case SERVICE_TYPE_OPEN_ID:
                cases = aiService.generate(prompt.getModelId(), json,false, prompt.getContext(), AiCaseList.class);
                break;
            case SERVICE_TYPE_OLLAMA:
                String ollamaModule = aiInferenceModelResolver.resolveOllamaModelName(prompt.getModelId());
                cases = aiService.generate(ollamaModule, json,false, prompt.getContext(), AiCaseList.class);
                break;
        }
        return success(cases);
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        System.out.println(PromptUtils.objectToPrompt(AiCaseList.class));
    }
}
