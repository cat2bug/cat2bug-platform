package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.AiInferenceModelResolver;
import com.cat2bug.admin.service.OpenAiGenerateService;
import com.cat2bug.admin.service.OllamaGenerateService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.domain.SysVersion;
import com.cat2bug.common.domain.vo.ai.AiDefectMember;
import com.cat2bug.common.domain.vo.ai.AiDefectModule;
import com.cat2bug.common.domain.vo.ai.AiDefectTitle;
import com.cat2bug.common.domain.vo.ai.AiDefectType;
import com.cat2bug.common.domain.vo.ai.AiDefectVersion;
import com.cat2bug.common.domain.vo.ai.AiDescribe;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDefectService;
import com.cat2bug.system.service.SysModuleService;
import com.cat2bug.system.service.SysUserConfigService;
import com.cat2bug.system.service.SysUserProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/ai/defect")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AiDefectResource {

    private static final String SERVICE_OPENAI = AiInferenceModelResolver.SERVICE_OPENAI;

    @Inject
    OllamaGenerateService ollamaGenerateService;

    @Inject
    OpenAiGenerateService openAiGenerateService;

    @Inject
    AiInferenceModelResolver aiInferenceModelResolver;

    @Inject
    SysModuleService sysModuleService;

    @Inject
    SysUserConfigService sysUserConfigService;

    @Inject
    SysUserProjectService sysUserProjectService;

    @Inject
    SysDefectService sysDefectService;

    @Inject
    AuthContext authContext;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @RequiresPermissions("system:defect:add")
    public AjaxResult auto(AiDescribe describe) throws Exception {
        AjaxResult bad = guardAi(describe);
        if (bad != null) {
            return bad;
        }
        String moduleKey = moduleKeyFor(describe);
        String prompt = String.format(
                "请根据( %s )的描述，生成一个核心思想的标题,标题只能有中英文或数字,不要有其他任何符号或字符，且最多128个字符",
                describe);
        AiDefectTitle title = generate(moduleKey, describe, prompt, null, AiDefectTitle.class);
        SysDefect defect = new SysDefect();
        defect.setDefectName(title.getTitle());
        defect.setDefectDescribe(describe.getDescribe());
        return AjaxResult.success(defect);
    }

    @POST
    @Path("/title")
    @RequiresPermissions("system:defect:add")
    public AjaxResult makeTitle(AiDescribe describe) throws Exception {
        AjaxResult bad = guardAi(describe);
        if (bad != null) {
            return bad;
        }
        String moduleKey = moduleKeyFor(describe);
        String prompt = String.format(
                "请根据( %s )的描述，生成一个标题,标题只能有中英文或数字,不要有其他任何符号或字符",
                describe);
        AiDefectTitle title = generate(moduleKey, describe, prompt, null, AiDefectTitle.class);
        return AjaxResult.success(title);
    }

    @POST
    @Path("/module")
    @RequiresPermissions("system:defect:add")
    public AjaxResult makeModule(AiDescribe describe) throws Exception {
        AjaxResult bad = guardAi(describe);
        if (bad != null) {
            return bad;
        }
        String moduleKey = moduleKeyFor(describe);
        SysUserConfig userConfig = requireUserConfig();
        List<Map<String, Object>> modules = sysModuleService.selectSysModulePathList(userConfig.getCurrentProjectId())
                .stream()
                .map(this::moduleToMap)
                .collect(Collectors.toList());
        if (modules.isEmpty()) {
            return AjaxResult.success();
        }
        String prompt = String.format(
                "请根据JSON( %s )数组中的数据，以及描述的信息( %s )，选择一个最符合的moduleId",
                objectMapper.writeValueAsString(modules), describe);
        AiDefectModule module = generate(moduleKey, describe, prompt, null, AiDefectModule.class);
        return AjaxResult.success(module);
    }

    @POST
    @Path("/type")
    @RequiresPermissions("system:defect:add")
    public AjaxResult makeType(AiDescribe describe) throws Exception {
        AjaxResult bad = guardAi(describe);
        if (bad != null) {
            return bad;
        }
        String moduleKey = moduleKeyFor(describe);
        List<String> types = Arrays.asList(DefectConstants.DEFECT_TYPES);
        String prompt = String.format(
                "请根据JSON( %s )数组中的数据，以及描述的信息( %s )，选择一个最符合的类型type，一般选择BUG的概率不较大",
                objectMapper.writeValueAsString(types), describe);
        AiDefectType type = generate(moduleKey, describe, prompt, null, AiDefectType.class);
        return AjaxResult.success(type);
    }

    @POST
    @Path("/member")
    @RequiresPermissions("system:defect:add")
    public AjaxResult makeMember(AiDescribe describe) throws Exception {
        AjaxResult bad = guardAi(describe);
        if (bad != null) {
            return bad;
        }
        String moduleKey = moduleKeyFor(describe);
        SysUserConfig userConfig = requireUserConfig();
        List<Map<String, Object>> members = sysUserProjectService
                .selectSysUserListByProjectId(userConfig.getCurrentProjectId())
                .stream()
                .map(this::memberToMap)
                .collect(Collectors.toList());
        String prompt = String.format(
                "请根据JSON( %s )数组中的成员数据，以及描述的信息( %s )，选择一个最符合的成员memberId",
                objectMapper.writeValueAsString(members), describe);
        AiDefectMember member = generate(moduleKey, describe, prompt, null, AiDefectMember.class);
        return AjaxResult.success(member);
    }

    @POST
    @Path("/version")
    @RequiresPermissions("system:defect:add")
    public AjaxResult makeVersion(AiDescribe describe) throws Exception {
        AjaxResult bad = guardAi(describe);
        if (bad != null) {
            return bad;
        }
        String moduleKey = moduleKeyFor(describe);
        SysUserConfig userConfig = requireUserConfig();
        List<SysVersion> versions = sysDefectService.selectVersionList(userConfig.getCurrentProjectId());
        if (versions == null || versions.isEmpty()) {
            return AjaxResult.success(new AiDefectVersion("1.0.0"));
        }
        String prompt = String.format(
                "请根据JSON( %s )数组中的历史版本，给我一个最新的version,最新的createTime如果超过两周了新建一个version",
                objectMapper.writeValueAsString(versions));
        AiDefectVersion version = generate(moduleKey, describe, prompt, null, AiDefectVersion.class);
        return AjaxResult.success(version);
    }

    private AjaxResult guardAi(AiDescribe describe) {
        if (describe == null || describe.getProjectId() == null) {
            return AjaxResult.error("项目 ID 不能为空");
        }
        String serviceType = aiInferenceModelResolver.normalizeServiceType(describe.getServiceType());
        if (SERVICE_OPENAI.equals(serviceType)) {
            if (!openAiGenerateService.isAvailable()) {
                return AjaxResult.error("当前环境未启用对应的 AI 服务：" + serviceType);
            }
        } else if (!ollamaGenerateService.isAvailable()) {
            return AjaxResult.error("当前环境未启用对应的 AI 服务：ollama");
        }
        String moduleKey = moduleKeyFor(describe);
        if (StringUtils.isBlank(moduleKey)) {
            return AjaxResult.error("未找到可用的 AI 模型，请在描述框旁选择模型，或先在项目中配置 Ollama / OpenAI 账号。");
        }
        return null;
    }

    private String moduleKeyFor(AiDescribe describe) {
        String serviceType = aiInferenceModelResolver.normalizeServiceType(describe.getServiceType());
        return SERVICE_OPENAI.equals(serviceType)
                ? aiInferenceModelResolver.resolveOpenAiAccountId(describe.getModelId(), describe.getProjectId())
                : aiInferenceModelResolver.resolveOllamaModelName(describe.getModelId());
    }

    private SysUserConfig requireUserConfig() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return sysUserConfigService.selectSysUserConfigByUserId(loginUser.getUserId());
    }

    private Map<String, Object> moduleToMap(SysModule module) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", module.getModulePath());
        row.put("moduleId", module.getModuleId());
        return row;
    }

    private Map<String, Object> memberToMap(SysUser user) {
        Map<String, Object> row = new HashMap<>();
        row.put("memberId", user.getUserId());
        row.put("memberRoles", user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toList()));
        return row;
    }

    private <T> T generate(String moduleKey, AiDescribe describe, String prompt, long[] context, Class<T> cls)
            throws Exception {
        String serviceType = aiInferenceModelResolver.normalizeServiceType(describe.getServiceType());
        if (SERVICE_OPENAI.equals(serviceType)) {
            return openAiGenerateService.generate(moduleKey, prompt, false, context, cls);
        }
        return ollamaGenerateService.generate(moduleKey, prompt, false, context, cls);
    }
}
