package com.cat2bug.web.controller.ai;

import com.alibaba.fastjson.JSON;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.domain.SysVersion;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.service.ISysUserProjectService;
import com.cat2bug.system.service.impl.SysDefectServiceImpl;
import com.cat2bug.web.vo.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Autowired
    private SysDefectServiceImpl sysDefectService;

    /**
     * 获取缺陷详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @PostMapping("/title")
    public AjaxResult makeTitle(@RequestBody String describe)
    {
        String prompt = String.format("请根据( %s )的描述，生成一个标题,标题只能有中英文或数字,不要有其他任何符号或字符",describe);
        AiDefectTitle title = aiService.generate("llama3:8b",prompt,false, null, AiDefectTitle.class);
        return success(title);
    }

    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @PostMapping("/module")
    public AjaxResult makeModule(@RequestBody String describe)
    {
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(getUserId());
        List<Map<String, Object>> modules = sysModuleService.selectSysModulePathList(userConfig.getCurrentProjectId()).stream().map(m->{
            Map<String,Object> module = new HashMap<>();
            module.put("name",m.getModulePath());
            module.put("moduleId",m.getModuleId());
            return module;
        }).collect(Collectors.toList());
        String prompt = String.format("请根据JSON( %s )数组中的数据，以及描述的信息( %s )，选择一个最符合的moduleId",JSON.toJSONString(modules),describe);
        AiDefectModule module = aiService.generate("llama3:8b",prompt,false, null, AiDefectModule.class);
        return success(module);
    }

    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @PostMapping("/type")
    public AjaxResult makeType(@RequestBody String describe)
    {
        List<String> types = Arrays.stream(SysDefectTypeEnum.values()).map(t->t.name()).collect(Collectors.toList());
        String prompt = String.format("请根据JSON( %s )数组中的数据，以及描述的信息( %s )，选择一个最符合的类型type",JSON.toJSONString(types),describe);
        AiDefectType type = aiService.generate("llama3:8b",prompt,false, null, AiDefectType.class);
        return success(type);
    }

    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @PostMapping("/member")
    public AjaxResult makeMember(@RequestBody String describe)
    {
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(getUserId());
        List<Map<String, Object>> members = sysUserProjectService.selectSysUserListByProjectId(userConfig.getCurrentProjectId(),new SysUser()).stream().map(m->{
            Map<String,Object> member = new HashMap<>();
            member.put("memberId",m.getUserId());
            member.put("memberRoles",m.getRoles().stream().map(r->r.getRoleName()));
            return member;
        }).collect(Collectors.toList());
        String prompt = String.format("请根据JSON( %s )数组中的成员数据，以及描述的信息( %s )，选择一个最符合的成员memberId",JSON.toJSONString(members),describe);
        AiDefectMember member = aiService.generate("llama3:8b",prompt,false, null, AiDefectMember.class);
        return success(member);
    }

    @PreAuthorize("@ss.hasPermi('system:defect:add')")
    @PostMapping("/version")
    public AjaxResult makeVersion()
    {
        SysUserConfig userConfig = sysUserConfigService.selectSysUserConfigByUserId(getUserId());
        List<SysVersion> versions = sysDefectService.selectVersionList(userConfig.getCurrentProjectId());
        if(versions==null || versions.size()==0) {
            return success(new AiDefectVersion("1.0.0"));
        } else {
            String prompt = String.format("请根据JSON( %s )数组中的历史版本，猜测一下本次系统的版本号，根据版本的维护时间来判断是用最近一次，还是新创建一个版本号，一般一两周内的用最近一次的版本号，超过这个时间的需要新建版本号，如果是新创建版本号，请参考历史版本的规律创建新版本号", JSON.toJSONString(versions));
            AiDefectVersion version = aiService.generate("llama3:8b", prompt, false, null, AiDefectVersion.class);
            return success(version);
        }
    }
}
