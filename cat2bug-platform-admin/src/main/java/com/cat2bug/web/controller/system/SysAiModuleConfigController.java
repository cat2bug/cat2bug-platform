package com.cat2bug.web.controller.system;

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
import com.cat2bug.system.domain.SysAiModuleConfig;
import com.cat2bug.system.service.ISysAiModuleConfigService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.function.Function;
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
    private IAiService aiService;

    /**
     * 推荐可使用的模型列表，配置文件
     */
    private List<String> models;
    /**
     * 配置项：默认使用的业务模型名
     */
    private String defaultBusinessModel;
    /**
     * 配置项：默认使用的图片处理模型名
     */
    private String defaultImageModel;

    /**
     * 查询AI模型配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:ai:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysAiModuleConfig sysAiModuleConfig)
    {
        SysAiModuleConfig config = sysAiModuleConfigService.selectSysAiModuleConfigByProjectId(sysAiModuleConfig.getProjectId());
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
                    if(config!=null) {
                        if(m.getName().equals(config.getBusinessModule())) {
                            map.put("businessModule", config.getBusinessModule());
                        }
                        if(m.getName().equals(config.getImageModule())) {
                            map.put("imageModule", config.getImageModule());
                        }
                    }
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
     * 获取默认模型列表
     * @return
     */
    private List<String> getDefaultModelList() {
        return Arrays.asList(defaultBusinessModel,defaultImageModel).stream().filter(m-> StringUtils.isNotBlank(m)).collect(Collectors.toList());
    }
}
