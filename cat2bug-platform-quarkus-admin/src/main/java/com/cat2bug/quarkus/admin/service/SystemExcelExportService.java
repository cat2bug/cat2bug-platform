package com.cat2bug.quarkus.admin.service;

import com.cat2bug.quarkus.common.domain.AiAccount;
import com.cat2bug.quarkus.common.domain.SysAiModuleConfig;
import com.cat2bug.quarkus.common.domain.SysConfig;
import com.cat2bug.quarkus.common.domain.SysDefectLog;
import com.cat2bug.quarkus.common.domain.SysDictData;
import com.cat2bug.quarkus.common.domain.SysDictType;
import com.cat2bug.quarkus.common.domain.SysDocument;
import com.cat2bug.quarkus.common.domain.SysLogininfor;
import com.cat2bug.quarkus.common.domain.SysModule;
import com.cat2bug.quarkus.common.domain.SysOperLog;
import com.cat2bug.quarkus.common.domain.SysPlan;
import com.cat2bug.quarkus.common.domain.SysPlanItem;
import com.cat2bug.quarkus.common.domain.SysPost;
import com.cat2bug.quarkus.common.domain.SysProject;
import com.cat2bug.quarkus.common.domain.SysProjectApi;
import com.cat2bug.quarkus.common.domain.SysReport;
import com.cat2bug.quarkus.common.domain.SysReportTemplate;
import com.cat2bug.quarkus.common.domain.SysRole;
import com.cat2bug.quarkus.common.domain.SysScreenSize;
import com.cat2bug.quarkus.common.domain.SysTeam;
import com.cat2bug.quarkus.common.domain.SysUserStatisticTemplate;
import com.cat2bug.quarkus.system.service.AiAccountService;
import com.cat2bug.quarkus.system.service.SysAiModuleConfigService;
import com.cat2bug.quarkus.system.service.SysConfigService;
import com.cat2bug.quarkus.system.service.SysDefectLogService;
import com.cat2bug.quarkus.system.service.SysDictDataService;
import com.cat2bug.quarkus.system.service.SysDictTypeService;
import com.cat2bug.quarkus.system.service.SysDocumentService;
import com.cat2bug.quarkus.system.service.SysLogininforService;
import com.cat2bug.quarkus.system.service.SysModuleService;
import com.cat2bug.quarkus.system.service.SysOperLogService;
import com.cat2bug.quarkus.system.service.SysPlanItemService;
import com.cat2bug.quarkus.system.service.SysPlanService;
import com.cat2bug.quarkus.system.service.SysPostService;
import com.cat2bug.quarkus.system.service.SysProjectApiService;
import com.cat2bug.quarkus.system.service.SysProjectService;
import com.cat2bug.quarkus.system.service.SysReportService;
import com.cat2bug.quarkus.system.service.SysReportTemplateService;
import com.cat2bug.quarkus.system.service.SysRoleService;
import com.cat2bug.quarkus.system.service.SysScreenSizeService;
import com.cat2bug.quarkus.system.service.SysTeamService;
import com.cat2bug.quarkus.system.service.SysUserStatisticTemplateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SystemExcelExportService {

    private static final List<SimpleExcelExportSupport.ColumnDef<SysRole>> ROLE_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("角色序号", r -> SimpleExcelExportSupport.text(r.getRoleId())),
            new SimpleExcelExportSupport.ColumnDef<>("角色名称", SysRole::getRoleName),
            new SimpleExcelExportSupport.ColumnDef<>("角色权限", SysRole::getRoleKey),
            new SimpleExcelExportSupport.ColumnDef<>("角色排序", r -> SimpleExcelExportSupport.text(r.getRoleSort())),
            new SimpleExcelExportSupport.ColumnDef<>("数据范围", r -> SimpleExcelExportSupport.dataScopeLabel(r.getDataScope())),
            new SimpleExcelExportSupport.ColumnDef<>("角色状态", r -> SimpleExcelExportSupport.statusLabel(r.getStatus()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysDictData>> DICT_DATA_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("字典编码", d -> SimpleExcelExportSupport.text(d.getDictCode())),
            new SimpleExcelExportSupport.ColumnDef<>("字典排序", d -> SimpleExcelExportSupport.text(d.getDictSort())),
            new SimpleExcelExportSupport.ColumnDef<>("字典标签", SysDictData::getDictLabel),
            new SimpleExcelExportSupport.ColumnDef<>("字典键值", SysDictData::getDictValue),
            new SimpleExcelExportSupport.ColumnDef<>("字典类型", SysDictData::getDictType),
            new SimpleExcelExportSupport.ColumnDef<>("是否默认", d -> SimpleExcelExportSupport.yesNoLabel(d.getIsDefault())),
            new SimpleExcelExportSupport.ColumnDef<>("状态", d -> SimpleExcelExportSupport.statusLabel(d.getStatus()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysDictType>> DICT_TYPE_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("字典主键", t -> SimpleExcelExportSupport.text(t.getDictId())),
            new SimpleExcelExportSupport.ColumnDef<>("字典名称", SysDictType::getDictName),
            new SimpleExcelExportSupport.ColumnDef<>("字典类型", SysDictType::getDictType),
            new SimpleExcelExportSupport.ColumnDef<>("状态", t -> SimpleExcelExportSupport.statusLabel(t.getStatus()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysDocument>> DOCUMENT_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("项目ID", d -> SimpleExcelExportSupport.text(d.getProjectId())),
            new SimpleExcelExportSupport.ColumnDef<>("文档名称", SysDocument::getDocName),
            new SimpleExcelExportSupport.ColumnDef<>("文档类型(0=文件夹；1=文件)", d -> SimpleExcelExportSupport.text(d.getDocType())),
            new SimpleExcelExportSupport.ColumnDef<>("文件类型", SysDocument::getFileExtension),
            new SimpleExcelExportSupport.ColumnDef<>("创建人ID", d -> SimpleExcelExportSupport.text(d.getCreateById())),
            new SimpleExcelExportSupport.ColumnDef<>("更新人", SysDocument::getUpdateBy),
            new SimpleExcelExportSupport.ColumnDef<>("文档版本", d -> SimpleExcelExportSupport.text(d.getFileVersion())),
            new SimpleExcelExportSupport.ColumnDef<>("文件夹ID", d -> SimpleExcelExportSupport.text(d.getDocPid())),
            new SimpleExcelExportSupport.ColumnDef<>("备注", SysDocument::getDocRemakr),
            new SimpleExcelExportSupport.ColumnDef<>("文件", SysDocument::getFileUrl)
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysUserStatisticTemplate>> STATISTIC_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("统计模版编码", SysUserStatisticTemplate::getStatisticTemplatCode),
            new SimpleExcelExportSupport.ColumnDef<>("模型类型", t -> SimpleExcelExportSupport.text(t.getModuleType())),
            new SimpleExcelExportSupport.ColumnDef<>("项目id", t -> SimpleExcelExportSupport.text(t.getProjectId())),
            new SimpleExcelExportSupport.ColumnDef<>("用户id", t -> SimpleExcelExportSupport.text(t.getUserId())),
            new SimpleExcelExportSupport.ColumnDef<>("统计模版配置", SysUserStatisticTemplate::getStatisticTemplatConfig)
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysAiModuleConfig>> AI_MODULE_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("项目ID", c -> SimpleExcelExportSupport.text(c.getProjectId()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysConfig>> CONFIG_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("参数主键", c -> SimpleExcelExportSupport.text(c.getConfigId())),
            new SimpleExcelExportSupport.ColumnDef<>("参数名称", SysConfig::getConfigName),
            new SimpleExcelExportSupport.ColumnDef<>("参数键名", SysConfig::getConfigKey),
            new SimpleExcelExportSupport.ColumnDef<>("参数键值", SysConfig::getConfigValue),
            new SimpleExcelExportSupport.ColumnDef<>("系统内置", c -> SimpleExcelExportSupport.yesNoLabel(c.getConfigType()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysPlan>> PLAN_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("测试计划名称", SysPlan::getPlanName),
            new SimpleExcelExportSupport.ColumnDef<>("测试默认版本", SysPlan::getPlanVersion),
            new SimpleExcelExportSupport.ColumnDef<>("计划开始时间", p -> formatDateTime(p.getPlanStartTime())),
            new SimpleExcelExportSupport.ColumnDef<>("计划结束时间", p -> formatDateTime(p.getPlanEndTime())),
            new SimpleExcelExportSupport.ColumnDef<>("更新人ID", p -> SimpleExcelExportSupport.text(p.getUpdateById())),
            new SimpleExcelExportSupport.ColumnDef<>("项目ID", p -> SimpleExcelExportSupport.text(p.getProjectId())),
            new SimpleExcelExportSupport.ColumnDef<>("报告ID", p -> SimpleExcelExportSupport.text(p.getReportId()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysPlanItem>> PLAN_ITEM_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("测试计划", SysPlanItem::getPlanId),
            new SimpleExcelExportSupport.ColumnDef<>("交付物", i -> SimpleExcelExportSupport.text(i.getModuleId())),
            new SimpleExcelExportSupport.ColumnDef<>("测试用例", i -> SimpleExcelExportSupport.text(i.getCaseId())),
            new SimpleExcelExportSupport.ColumnDef<>("计划子项状态", SysPlanItem::getPlanItemState),
            new SimpleExcelExportSupport.ColumnDef<>("更新人", i -> SimpleExcelExportSupport.text(i.getUpdateById())),
            new SimpleExcelExportSupport.ColumnDef<>("缺陷", i -> formatDefectIds(i.getDefectIds()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<AiAccount>> AI_ACCOUNT_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("AI服务网址", AiAccount::getAiUrl),
            new SimpleExcelExportSupport.ColumnDef<>("模型名称", AiAccount::getModelName),
            new SimpleExcelExportSupport.ColumnDef<>("最大Token", a -> SimpleExcelExportSupport.text(a.getMaxCompletionTokens())),
            new SimpleExcelExportSupport.ColumnDef<>("密钥", AiAccount::getApiKey),
            new SimpleExcelExportSupport.ColumnDef<>("账号名称", AiAccount::getAccountName)
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysModule>> MODULE_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("父模块id", m -> SimpleExcelExportSupport.text(m.getModulePid())),
            new SimpleExcelExportSupport.ColumnDef<>("模块名称", SysModule::getModuleName),
            new SimpleExcelExportSupport.ColumnDef<>("项目id", m -> SimpleExcelExportSupport.text(m.getProjectId()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysTeam>> TEAM_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("团队名称", SysTeam::getTeamName),
            new SimpleExcelExportSupport.ColumnDef<>("团队图标", SysTeam::getTeamIcon),
            new SimpleExcelExportSupport.ColumnDef<>("团队介绍", SysTeam::getIntroduce),
            new SimpleExcelExportSupport.ColumnDef<>("项目数量", t -> SimpleExcelExportSupport.text(t.getProjectCount())),
            new SimpleExcelExportSupport.ColumnDef<>("成员数量", t -> SimpleExcelExportSupport.text(t.getMemberCount())),
            new SimpleExcelExportSupport.ColumnDef<>("是否锁定", t -> SimpleExcelExportSupport.yesNoLabel(
                    t.getLock() != null && t.getLock() ? "Y" : "N")),
            new SimpleExcelExportSupport.ColumnDef<>("锁定备注", SysTeam::getLockRemark)
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysProject>> PROJECT_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("项目名称", SysProject::getProjectName),
            new SimpleExcelExportSupport.ColumnDef<>("项目图标地址", SysProject::getProjectIcon),
            new SimpleExcelExportSupport.ColumnDef<>("项目介绍", SysProject::getProjectIntroduce),
            new SimpleExcelExportSupport.ColumnDef<>("是否锁定", p -> SimpleExcelExportSupport.yesNoLabel(
                    p.getLock() != null && p.getLock() ? "Y" : "N")),
            new SimpleExcelExportSupport.ColumnDef<>("锁定备注", SysProject::getLockRemark)
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysDefectLog>> DEFECT_LOG_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("缺陷日志的描述", SysDefectLog::getDefectLogDescribe),
            new SimpleExcelExportSupport.ColumnDef<>("缺陷接收人", log -> formatDefectIds(log.getReceiveBy())),
            new SimpleExcelExportSupport.ColumnDef<>("附件集合", SysDefectLog::getAnnexUrls),
            new SimpleExcelExportSupport.ColumnDef<>("缺陷id", log -> SimpleExcelExportSupport.text(log.getDefectId()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysPost>> POST_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("岗位序号", p -> SimpleExcelExportSupport.text(p.getPostId())),
            new SimpleExcelExportSupport.ColumnDef<>("岗位编码", SysPost::getPostCode),
            new SimpleExcelExportSupport.ColumnDef<>("岗位名称", SysPost::getPostName),
            new SimpleExcelExportSupport.ColumnDef<>("岗位排序", p -> SimpleExcelExportSupport.text(p.getPostSort())),
            new SimpleExcelExportSupport.ColumnDef<>("状态", p -> "0".equals(p.getStatus()) ? "正常" : "停用")
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysReport>> REPORT_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("报告标题", SysReport::getReportTitle),
            new SimpleExcelExportSupport.ColumnDef<>("报告时间", r -> formatReportDate(r.getReportTime())),
            new SimpleExcelExportSupport.ColumnDef<>("报告描述", SysReport::getReportDescription),
            new SimpleExcelExportSupport.ColumnDef<>("数据", r -> r.getReportData() == null ? "" : String.valueOf(r.getReportData())),
            new SimpleExcelExportSupport.ColumnDef<>("数据来源", SysReport::getReportSource),
            new SimpleExcelExportSupport.ColumnDef<>(" 推送人ID", r -> SimpleExcelExportSupport.text(r.getCreateById()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysReportTemplate>> REPORT_TEMPLATE_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("交付物类型", SysReportTemplate::getModuleType),
            new SimpleExcelExportSupport.ColumnDef<>("模版内容", SysReportTemplate::getTemplateContent),
            new SimpleExcelExportSupport.ColumnDef<>("模版标题", SysReportTemplate::getTemplateTitle),
            new SimpleExcelExportSupport.ColumnDef<>("更新用户", t -> SimpleExcelExportSupport.text(t.getUpdateById())),
            new SimpleExcelExportSupport.ColumnDef<>("项目ID", t -> SimpleExcelExportSupport.text(t.getProjectId())),
            new SimpleExcelExportSupport.ColumnDef<>("模版图标路径", SysReportTemplate::getTemplateIconUrl),
            new SimpleExcelExportSupport.ColumnDef<>("主版本", t -> SimpleExcelExportSupport.text(t.getMajorVersion())),
            new SimpleExcelExportSupport.ColumnDef<>("次版本", t -> SimpleExcelExportSupport.text(t.getMinorVersion())),
            new SimpleExcelExportSupport.ColumnDef<>("模版唯一标识", SysReportTemplate::getTemplateKey)
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysProjectApi>> PROJECT_API_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("项目id", a -> SimpleExcelExportSupport.text(a.getProjectId())),
            new SimpleExcelExportSupport.ColumnDef<>("用户id", a -> SimpleExcelExportSupport.text(a.getUserId())),
            new SimpleExcelExportSupport.ColumnDef<>("白名单", a -> formatStringList(a.getWhiteList())),
            new SimpleExcelExportSupport.ColumnDef<>("有效时间", a -> formatApiDate(a.getExpireTime())),
            new SimpleExcelExportSupport.ColumnDef<>("API名称", SysProjectApi::getApiName),
            new SimpleExcelExportSupport.ColumnDef<>("启用状态", a -> a.getEnabled() != null && a.getEnabled() ? "是" : "否"),
            new SimpleExcelExportSupport.ColumnDef<>("权限配置", SysProjectApi::getFeatures)
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysOperLog>> OPER_LOG_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("操作序号", r -> SimpleExcelExportSupport.text(r.getOperId())),
            new SimpleExcelExportSupport.ColumnDef<>("操作模块", SysOperLog::getTitle),
            new SimpleExcelExportSupport.ColumnDef<>("业务类型", r -> businessTypeLabel(r.getBusinessType())),
            new SimpleExcelExportSupport.ColumnDef<>("请求方法", SysOperLog::getMethod),
            new SimpleExcelExportSupport.ColumnDef<>("请求方式", SysOperLog::getRequestMethod),
            new SimpleExcelExportSupport.ColumnDef<>("操作类别", r -> operatorTypeLabel(r.getOperatorType())),
            new SimpleExcelExportSupport.ColumnDef<>("操作人员", SysOperLog::getOperName),
            new SimpleExcelExportSupport.ColumnDef<>("部门名称", SysOperLog::getDeptName),
            new SimpleExcelExportSupport.ColumnDef<>("请求地址", SysOperLog::getOperUrl),
            new SimpleExcelExportSupport.ColumnDef<>("操作地址", SysOperLog::getOperIp),
            new SimpleExcelExportSupport.ColumnDef<>("操作地点", SysOperLog::getOperLocation),
            new SimpleExcelExportSupport.ColumnDef<>("请求参数", SysOperLog::getOperParam),
            new SimpleExcelExportSupport.ColumnDef<>("返回参数", SysOperLog::getJsonResult),
            new SimpleExcelExportSupport.ColumnDef<>("状态", r -> operStatusLabel(r.getStatus())),
            new SimpleExcelExportSupport.ColumnDef<>("错误消息", SysOperLog::getErrorMsg),
            new SimpleExcelExportSupport.ColumnDef<>("操作时间", r -> formatDateTime(r.getOperTime())),
            new SimpleExcelExportSupport.ColumnDef<>("消耗时间", r -> r.getCostTime() == null ? "" : r.getCostTime() + "毫秒")
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysLogininfor>> LOGININFOR_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("序号", i -> SimpleExcelExportSupport.text(i.getInfoId())),
            new SimpleExcelExportSupport.ColumnDef<>("用户账号", SysLogininfor::getUserName),
            new SimpleExcelExportSupport.ColumnDef<>("登录状态", i -> loginStatusLabel(i.getStatus())),
            new SimpleExcelExportSupport.ColumnDef<>("登录地址", SysLogininfor::getIpaddr),
            new SimpleExcelExportSupport.ColumnDef<>("登录地点", SysLogininfor::getLoginLocation),
            new SimpleExcelExportSupport.ColumnDef<>("浏览器", SysLogininfor::getBrowser),
            new SimpleExcelExportSupport.ColumnDef<>("操作系统", SysLogininfor::getOs),
            new SimpleExcelExportSupport.ColumnDef<>("提示消息", SysLogininfor::getMsg),
            new SimpleExcelExportSupport.ColumnDef<>("访问时间", i -> formatDateTime(i.getLoginTime()))
    );

    private static final List<SimpleExcelExportSupport.ColumnDef<SysScreenSize>> SCREEN_SIZE_COLUMNS = List.of(
            new SimpleExcelExportSupport.ColumnDef<>("名称", SysScreenSize::getName),
            new SimpleExcelExportSupport.ColumnDef<>("宽", SysScreenSize::getWidth),
            new SimpleExcelExportSupport.ColumnDef<>("高", SysScreenSize::getHeight)
    );

    private static final SimpleDateFormat PLAN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat REPORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Inject
    SysRoleService sysRoleService;

    @Inject
    SysDictDataService sysDictDataService;

    @Inject
    SysDictTypeService sysDictTypeService;

    @Inject
    SysDocumentService sysDocumentService;

    @Inject
    SysUserStatisticTemplateService sysUserStatisticTemplateService;

    @Inject
    SysAiModuleConfigService sysAiModuleConfigService;

    @Inject
    SysConfigService sysConfigService;

    @Inject
    SysPlanService sysPlanService;

    @Inject
    SysPlanItemService sysPlanItemService;

    @Inject
    AiAccountService aiAccountService;

    @Inject
    SysModuleService sysModuleService;

    @Inject
    SysTeamService sysTeamService;

    @Inject
    SysProjectService sysProjectService;

    @Inject
    SysDefectLogService sysDefectLogService;

    @Inject
    SysPostService sysPostService;

    @Inject
    SysReportService sysReportService;

    @Inject
    SysReportTemplateService sysReportTemplateService;

    @Inject
    SysProjectApiService sysProjectApiService;

    @Inject
    SysOperLogService sysOperLogService;

    @Inject
    SysLogininforService sysLogininforService;

    @Inject
    SysScreenSizeService sysScreenSizeService;

    public byte[] exportRoles(SysRole query) {
        return SimpleExcelExportSupport.export("角色数据", ROLE_COLUMNS, sysRoleService.selectRoleList(query), "导出角色数据失败");
    }

    public byte[] exportDictData(SysDictData query) {
        return SimpleExcelExportSupport.export("字典数据", DICT_DATA_COLUMNS,
                sysDictDataService.selectDictDataList(query), "导出字典数据失败");
    }

    public byte[] exportDictTypes(SysDictType query) {
        return SimpleExcelExportSupport.export("字典类型", DICT_TYPE_COLUMNS,
                sysDictTypeService.selectDictTypeList(query), "导出字典类型失败");
    }

    public byte[] exportDocuments(SysDocument query) {
        return SimpleExcelExportSupport.export("文档数据", DOCUMENT_COLUMNS,
                sysDocumentService.selectDocumentListAll(query), "导出文档数据失败");
    }

    public byte[] exportStatisticTemplates(SysUserStatisticTemplate query) {
        return SimpleExcelExportSupport.export("用户统计模版数据", STATISTIC_COLUMNS,
                sysUserStatisticTemplateService.selectTemplateListAll(query), "导出用户统计模版失败");
    }

    public byte[] exportAiModuleConfigs(SysAiModuleConfig query) {
        List<SysAiModuleConfig> list = sysAiModuleConfigService.selectSysAiModuleConfigList(query);
        return SimpleExcelExportSupport.export("AI模型配置数据", AI_MODULE_COLUMNS,
                list != null ? list : List.of(), "导出 AI 模型配置失败");
    }

    public byte[] exportConfigs(SysConfig query) {
        return SimpleExcelExportSupport.export("参数数据", CONFIG_COLUMNS,
                sysConfigService.selectConfigList(query), "导出参数数据失败");
    }

    public byte[] exportPlans(SysPlan query) {
        return SimpleExcelExportSupport.export("测试计划数据", PLAN_COLUMNS,
                sysPlanService.selectPlanListAll(query), "导出测试计划失败");
    }

    public byte[] exportPlanItems(SysPlanItem query) {
        return SimpleExcelExportSupport.export("测试计划子项数据", PLAN_ITEM_COLUMNS,
                sysPlanItemService.selectPlanItemListAll(query), "导出测试计划子项失败");
    }

    public byte[] exportAiAccounts(AiAccount query) {
        return SimpleExcelExportSupport.export("OpenAI账号数据", AI_ACCOUNT_COLUMNS,
                aiAccountService.selectAiAccountList(query), "导出 OpenAI 账号失败");
    }

    public byte[] exportModules(SysModule query) {
        return SimpleExcelExportSupport.export("模块数据", MODULE_COLUMNS,
                sysModuleService.selectSysModuleList(query), "导出模块数据失败");
    }

    public byte[] exportTeams(String teamName) {
        return SimpleExcelExportSupport.export("团队数据", TEAM_COLUMNS,
                sysTeamService.selectTeamListAll(teamName), "导出团队数据失败");
    }

    public byte[] exportProjects(Long userId, SysProject query) {
        return SimpleExcelExportSupport.export("项目数据", PROJECT_COLUMNS,
                sysProjectService.selectProjectListAll(userId, query), "导出项目数据失败");
    }

    public byte[] exportDefectLogs(SysDefectLog query) {
        return SimpleExcelExportSupport.export("缺陷日志数据", DEFECT_LOG_COLUMNS,
                sysDefectLogService.selectSysDefectLogList(query), "导出缺陷日志失败");
    }

    public byte[] exportPosts(SysPost query) {
        return SimpleExcelExportSupport.export("岗位数据", POST_COLUMNS,
                sysPostService.selectPostListAll(query), "导出岗位数据失败");
    }

    public byte[] exportReports(SysReport query) {
        return SimpleExcelExportSupport.export("报告数据", REPORT_COLUMNS,
                sysReportService.selectReportListAll(query), "导出报告数据失败");
    }

    public byte[] exportReportTemplates(SysReportTemplate query) {
        return SimpleExcelExportSupport.export("报告模版数据", REPORT_TEMPLATE_COLUMNS,
                sysReportTemplateService.selectTemplateListAll(query), "导出报告模版数据失败");
    }

    public byte[] exportProjectApis(SysProjectApi query) {
        return SimpleExcelExportSupport.export("项目API数据", PROJECT_API_COLUMNS,
                sysProjectApiService.selectProjectApiListAll(query), "导出项目API数据失败");
    }

    public byte[] exportOperLogs(SysOperLog query) {
        return SimpleExcelExportSupport.export("操作日志", OPER_LOG_COLUMNS,
                sysOperLogService.selectOperLogListAll(query), "导出操作日志失败");
    }

    public byte[] exportLogininfor(SysLogininfor query) {
        return SimpleExcelExportSupport.export("登录日志", LOGININFOR_COLUMNS,
                sysLogininforService.selectLogininforListAll(query), "导出登录日志失败");
    }

    public byte[] exportScreenSizes(SysScreenSize query) {
        return SimpleExcelExportSupport.export("屏幕尺寸数据", SCREEN_SIZE_COLUMNS,
                sysScreenSizeService.selectScreenSizeListAll(query), "导出屏幕尺寸失败");
    }

    private static String formatDefectIds(List<Long> defectIds) {
        if (defectIds == null || defectIds.isEmpty()) {
            return "";
        }
        return defectIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        synchronized (PLAN_DATE_FORMAT) {
            return PLAN_DATE_FORMAT.format(date);
        }
    }

    private static String formatReportDate(Date date) {
        if (date == null) {
            return "";
        }
        synchronized (REPORT_DATE_FORMAT) {
            return REPORT_DATE_FORMAT.format(date);
        }
    }

    private static String formatApiDate(Date date) {
        if (date == null) {
            return "";
        }
        synchronized (API_DATE_FORMAT) {
            return API_DATE_FORMAT.format(date);
        }
    }

    private static String formatStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }

    private static String businessTypeLabel(Integer businessType) {
        if (businessType == null) {
            return "";
        }
        return switch (businessType) {
            case 0 -> "其它";
            case 1 -> "新增";
            case 2 -> "修改";
            case 3 -> "删除";
            case 4 -> "授权";
            case 5 -> "导出";
            case 6 -> "导入";
            case 7 -> "强退";
            case 8 -> "生成代码";
            case 9 -> "清空数据";
            default -> String.valueOf(businessType);
        };
    }

    private static String operatorTypeLabel(Integer operatorType) {
        if (operatorType == null) {
            return "";
        }
        return switch (operatorType) {
            case 0 -> "其它";
            case 1 -> "后台用户";
            case 2 -> "手机端用户";
            default -> String.valueOf(operatorType);
        };
    }

    private static String operStatusLabel(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 0 -> "正常";
            case 1 -> "异常";
            default -> String.valueOf(status);
        };
    }

    private static String loginStatusLabel(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "0" -> "成功";
            case "1" -> "失败";
            default -> status;
        };
    }
}
