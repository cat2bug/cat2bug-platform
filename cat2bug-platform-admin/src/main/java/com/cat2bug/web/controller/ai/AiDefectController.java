package com.cat2bug.web.controller.ai;

import com.alibaba.fastjson.JSON;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.web.vo.AiDefect;
import com.cat2bug.web.vo.AiDefectModule;
import com.cat2bug.web.vo.AiDefectTitle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-13 22:57
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/ai/defect")
public class AiDefectController extends BaseController {
    private final static Logger log = LogManager.getLogger(AiDefectController.class);
    @Autowired
    private IAiService aiService;
    @Autowired
    private ISysModuleService sysModuleService;
    @Autowired
    private ISysUserConfigService sysUserConfigService;
    @Autowired
    private ISysUserProjectService sysUserProjectService;

    /**
     * 获取缺陷详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @GetMapping("/title")
    public AjaxResult makeTitle(String describe)
    {
        String prompt = String.format("请根据[%s]的描述，生成一个标题,标题只能有中英文或数字,不要有其他任何符号或字符",describe);
        AiDefectTitle title = aiService.generate("llama3:8b",prompt,false, AiDefectTitle.class);
        return success(title);
    }

    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @GetMapping("/module")
    public AjaxResult makeModule(String describe)
    {
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(getUserId());
        List<Map<String, Object>> modules = sysModuleService.selectSysModulePathList(userConfig.getCurrentProjectId()).stream().map(m->{
            Map<String,Object> module = new HashMap<>();
            module.put("name",m.getModulePath());
            module.put("moduleId",m.getModuleId());
            return module;
        }).collect(Collectors.toList());
        String prompt = String.format("请根据[%s]数组中的数据，以及描述的信息[%s]，选择一个最符合的moduleId",JSON.toJSONString(modules),describe);
        AiDefectModule module = aiService.generate("llama3:8b",prompt,false, AiDefectModule.class);
        return success(module);
    }

    /**
     * 获取缺陷详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @PostMapping
    public AjaxResult makeDefect(@RequestBody String describe)
    {
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(getUserId());
        Map<String, Object> params = new HashMap<>();
        params.put("describe",describe);
        params.put("defectTypes", Arrays.stream(SysDefectTypeEnum.values()).map(t->t.name()).collect(Collectors.toList()));
        List<Map<String, Object>> modules = sysModuleService.selectSysModulePathList(userConfig.getCurrentProjectId()).stream().map(m->{
            Map<String,Object> module = new HashMap<>();
            module.put("name",m.getModulePath());
            module.put("moduleId",m.getModuleId());
            return module;
        }).collect(Collectors.toList());
        params.put("modules",modules);
        List<Map<String, Object>> members = sysUserProjectService.selectSysUserListByProjectId(userConfig.getCurrentProjectId(),new SysUser()).stream().map(m->{
            Map<String,Object> member = new HashMap<>();
            member.put("memberId",m.getUserId());
            member.put("memberRoles",m.getRoles().stream().map(r->r.getRoleName()));
            return member;
        }).collect(Collectors.toList());
        params.put("members",members);
        StringBuffer prompt = new StringBuffer();
        prompt.append(JSON.toJSONString(params)+"\n");
        prompt.append("describe是描述，请根据以上所有参数生成JSON。");
        prompt.append("defectName根据describe总结生成;");
        prompt.append("moduleId根据modules和describe生成;");
        prompt.append("defectType根据defectTypes和describe生成;");
        prompt.append("handleBy根据members和describe生成，其中只选一个人的memberId即可;");
        log.info("生成defect对象,prompt:{}",prompt.toString());
        AiDefect defect = aiService.generate("llama3:8b",prompt.toString(),false, AiDefect.class);
        return success(defect);
    }
}
