package com.cat2bug.web.controller.ai;

import com.alibaba.fastjson.JSON;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.ai.utils.PromptUtils;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysAiModuleConfig;
import com.cat2bug.system.domain.SysCaseStep;
import com.cat2bug.system.service.ISysAiModuleConfigService;
import com.cat2bug.web.vo.AiCaseList;
import com.cat2bug.web.vo.AiPrompt;
import com.cat2bug.web.vo.AiSysPrompt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-15 02:26
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/ai/case")
public class AiCaseController extends BaseController {
    private final static Logger log = LogManager.getLogger(AiCaseController.class);
    @Autowired
    private IAiService aiService;
    @Autowired
    private ISysAiModuleConfigService sysAiModuleConfigService;
    /**
     * 查询测试用例列表
     */
    @PreAuthorize("@ss.hasPermi('system:case:add')")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody AiSysPrompt prompt)
    {
        SysAiModuleConfig sysAiModuleConfig = sysAiModuleConfigService.selectSysAiModuleConfigByProjectId(prompt.getProjectId());
        String json = String.format("%s。前面如果没有标明数量，那默认生成5条测试用例", prompt.getPrompt());
        AiCaseList cases = aiService.generate(sysAiModuleConfig.getBusinessModule(), json,false, prompt.getContext(), AiCaseList.class);
        return success(cases);
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        System.out.println(PromptUtils.objectToPrompt(AiCaseList.class));
    }
}
