package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.domain.ApiDeliverable;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.api.service.IApiDeliverableService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:19
 * @Version: 1.0.0
 */
@RestController
@RequestMapping("/api/deliverable")
public class ApiDeliverableController extends BaseController {
    @Resource
    private IApiDeliverableService apiDeliverableService;

    @Autowired
    private ISysModuleService sysModuleService;

    /**
     * 查询交付物列表
     * @param apiDeliverable 交付物参数对象
     * @return  交付物集合
     */
    @GetMapping
    public TableDataInfo list(ApiDeliverable apiDeliverable)
    {
        if(apiDeliverable.getDeliverablePid()==null) {
            apiDeliverable.setDeliverablePid(0L);
        }
        List<ApiDeliverable> list = this.apiDeliverableService.selectApiDeliverableList(apiDeliverable);
        return getDataTable(list);
    }

    /**
     * 查询交付物详情
     * @param moduleId 交付物ID
     * @return 交付物详情
     */
    @GetMapping("/{moduleId}")
    public AjaxResult getInfo(@PathVariable("moduleId") Long moduleId)
    {
        SysModule sysModule = sysModuleService.selectSysModuleByModuleId(moduleId);
        return AjaxResult.success(sysModule);
    }

    /**
     * 新增交付物
     * @param sysModule 交付物对象
     * @return 结果
     */
    @PostMapping
    public AjaxResult add(@RequestBody SysModule sysModule)
    {
        List<SysModule> result = sysModuleService.insertSysModule(sysModule);
        return AjaxResult.success(result);
    }
}
