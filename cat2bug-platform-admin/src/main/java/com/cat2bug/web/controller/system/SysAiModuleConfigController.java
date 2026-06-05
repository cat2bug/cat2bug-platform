package com.cat2bug.web.controller.system;

import com.cat2bug.ai.domain.AiAccount;
import com.cat2bug.ai.service.IAiAccountService;
import com.cat2bug.ai.service.IAiService;
import com.cat2bug.ai.vo.AiModule;
import com.cat2bug.ai.vo.AiModuleStateEnum;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysAiModuleConfig;
import com.cat2bug.system.service.ISysAiModuleConfigService;
import com.cat2bug.web.service.OllamaAvailability;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AI模型配置Controller
 * 
 * @author yuzhantao
 * @date 2024-06-20
 */
@RestController
@RequestMapping("/system/ai")
@ConfigurationProperties(prefix = "cat2bug.ai")
@Data
public class SysAiModuleConfigController extends BaseController
{
    @Autowired
    private ISysAiModuleConfigService sysAiModuleConfigService;

    @Autowired
    private IAiAccountService aiAccountService;

    private final static String SERVICE_TYPE_OLLAMA = "ollama";
    @Autowired(required = false)
    private Map<String, IAiService> aiServiceMap;

    @Autowired
    private OllamaAvailability ollamaAvailability;

    /**
     * 推荐可使用的模型列表，配置文件
     */
    private List<String> models;
    /**
     * 配置项：推荐拉取的默认业务模型名（仅用于列表中 EMPTY 占位，不再写入数据库列）
     */
    private String defaultBusinessModel;

    /**
     * 项目内 AI 推理可选模型：已下载 Ollama 模型 + OpenAI 账号（供用例/缺陷等下拉框使用）
     */
    @PreAuthorize("@ss.hasPermi('system:ai:list') || @ss.hasPermi('system:case:add') || @ss.hasPermi('system:defect:add')")
    @GetMapping("/project-model-options")
    public AjaxResult projectModelOptions(@RequestParam Long projectId)
    {
        IAiService aiService = aiServiceMap != null ? aiServiceMap.get(SERVICE_TYPE_OLLAMA) : null;
        List<Map<String, Object>> ollamaOpts = new ArrayList<>();
        if (aiService != null) {
            List<AiModule> downloadedList = aiService.getModuleList(AiModule.class);
            ollamaOpts = downloadedList.stream()
                    .filter(m -> m != null && m.getState() == AiModuleStateEnum.COMPLETED && StringUtils.isNotBlank(m.getName()))
                    .map(m -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("key", m.getName());
                        row.put("label", m.getName());
                        return row;
                    })
                    .collect(Collectors.toList());
        }
        AiAccount accQuery = new AiAccount();
        accQuery.setProjectId(projectId);
        List<AiAccount> accounts = aiAccountService != null ? aiAccountService.selectAiAccountList(accQuery) : Collections.emptyList();
        List<Map<String, Object>> openaiOpts = accounts.stream()
                .filter(a -> a != null && a.getAccountId() != null)
                .map(a -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    String key = String.valueOf(a.getAccountId());
                    row.put("key", key);
                    row.put("label", StringUtils.isNotBlank(a.getAccountName()) ? a.getAccountName() : key);
                    row.put("accountId", a.getAccountId());
                    return row;
                })
                .collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ollama", ollamaOpts);
        body.put("openai", openaiOpts);
        body.put("aiEnabled", ollamaAvailability.isAvailable());
        return success(body);
    }

    private IAiService requireOllamaService()
    {
        IAiService aiService = aiServiceMap != null ? aiServiceMap.get(SERVICE_TYPE_OLLAMA) : null;
        if (aiService == null)
        {
            throw new ServiceException("Ollama service is not enabled");
        }
        return aiService;
    }

    /**
     * 查询AI模型配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:ai:list') || @ss.hasPermi('system:case:add')")
    @GetMapping("/list")
    public TableDataInfo list(SysAiModuleConfig sysAiModuleConfig)
    {
        if (!ollamaAvailability.isAvailable())
        {
            return getDataTable(Collections.emptyList());
        }
        IAiService aiService = requireOllamaService();
        List<AiModule>  downloadedList = aiService.getModuleList(AiModule.class);
        List<Map<String, Object>> list = Stream.of(
                downloadedList,
                this.getDefaultModelList().stream().filter(m-> StringUtils.isNotBlank(m)).map(m->{
                    AiModule module = new AiModule();
                    module.setName(m);
                    module.setState(AiModuleStateEnum.EMPTY);
                    return module;
                }).collect(Collectors.toList())
        ).flatMap(Collection::stream)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(AiModule::getName))))
                .stream().map(m->{
                    Map<String, Object> map = new HashMap<>();
                    map.put("name",m.getName());
                    map.put("size",m.getSize());
                    map.put("state",m.getState());
                    return map;
                }).collect(Collectors.toList());
        return getDataTable(list);
    }

    /**
     * 查询AI模型配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:ai:list')")
    @GetMapping("/model/list/default")
    public TableDataInfo defaultModelList()
    {
        return getDataTable(Stream.of(this.getDefaultModelList(),this.models).flatMap(Collection::stream).distinct().collect(Collectors.toList()));
    }


    @PreAuthorize("@ss.hasPermi('system:ai:add')")
    @PostMapping("/model")
    public AjaxResult downloadModel(@RequestBody AiModule aiModule) {
        IAiService aiService = requireOllamaService();
        return success(aiService.pullModule(aiModule.getName(),true));
    }

    /**
     * 删除AI模型配置
     */
    @PreAuthorize("@ss.hasPermi('system:ai:remove')")
    @Log(title = "AI模型配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/model")
    public AjaxResult remove(@RequestBody AiModule aiModule)
    {
        IAiService aiService = requireOllamaService();
        return toAjax(aiService.removeModule(aiModule.getName()));
    }

    /**
     * 导出AI模型配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:ai:export')")
    @Log(title = "AI模型配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysAiModuleConfig sysAiModuleConfig)
    {
        List<SysAiModuleConfig> list = sysAiModuleConfigService.selectSysAiModuleConfigList(sysAiModuleConfig);
        ExcelUtil<SysAiModuleConfig> util = new ExcelUtil<SysAiModuleConfig>(SysAiModuleConfig.class);
        util.exportExcel(response, list, "AI模型配置数据");
    }

    /**
     * 获取AI模型配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:ai:query')")
    @GetMapping(value = "/{aiId}")
    public AjaxResult getInfo(@PathVariable("aiId") Long aiId)
    {
        return success(sysAiModuleConfigService.selectSysAiModuleConfigByAiId(aiId));
    }

    /**
     * 新增AI模型配置
     */
    @PreAuthorize("@ss.hasPermi('system:ai:add')")
    @Log(title = "AI模型配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysAiModuleConfig sysAiModuleConfig)
    {
        return toAjax(sysAiModuleConfigService.insertSysAiModuleConfig(sysAiModuleConfig));
    }

    /**
     * 修改AI模型配置
     */
    @PreAuthorize("@ss.hasPermi('system:ai:edit')")
    @Log(title = "AI模型配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysAiModuleConfig sysAiModuleConfig)
    {
        return toAjax(sysAiModuleConfigService.updateSysAiModuleConfig(sysAiModuleConfig));
    }

    /**
     * 删除AI模型配置
     */
    @PreAuthorize("@ss.hasPermi('system:ai:remove')")
    @Log(title = "AI模型配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{aiIds}")
    public AjaxResult remove(@PathVariable Long[] aiIds)
    {
        return toAjax(sysAiModuleConfigService.deleteSysAiModuleConfigByAiIds(aiIds));
    }

    /**
     * 获取默认模型列表（推荐下载的模型名）
     * @return
     */
    private List<String> getDefaultModelList() {
        return StringUtils.isNotBlank(defaultBusinessModel)
                ? Collections.singletonList(defaultBusinessModel)
                : Collections.emptyList();
    }
}
