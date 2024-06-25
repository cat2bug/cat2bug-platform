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
//        ，每条测试用例包括：名称caseName、用例级别caseLevel(1-5)、前置条件casePreconditions(至少描述5个字以上)、预期caseExpect(至少描述5个字以上)、测试步骤数组caseStep(至少3-5条测试步骤)，(每条测试步骤包括：步骤描述stepDescribe、步骤预期stepExpect，每条测试步骤的属性都必填)，每个用例属性都是必填项。
        String json = String.format("%s。生成10条测试用例", prompt.getPrompt());
        AiCaseList cases = aiService.generate(sysAiModuleConfig.getBusinessModule(), json,false, prompt.getContext(), AiCaseList.class);
        return success(cases);
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        System.out.println(PromptUtils.objectToPrompt(AiCaseList.class));
    }
}
