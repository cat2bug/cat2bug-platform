package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysPlanItem;
import com.cat2bug.system.domain.SysPlanItemModule;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.system.service.ISysModuleService;
import com.cat2bug.system.service.ISysPlanItemService;
import com.cat2bug.system.service.ISysUserConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 测试计划子项Controller
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
@RestController
@RequestMapping("/system/PlanItem")
public class SysPlanItemController extends BaseController
{
    @Autowired
    private ISysPlanItemService sysPlanItemService;
    @Autowired
    private ISysModuleService sysModuleService;

    /**
     * 查询测试计划子项列表
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @GetMapping("/list")
    public TableDataInfo list(SysPlanItem sysPlanItem)
    {
        if(sysPlanItem.getParams()==null) {
            sysPlanItem.setParams(new HashMap<>());
        }
        Set<Long> moduleIds = sysModuleService.getAllChildIds(sysPlanItem.getProjectId(), sysPlanItem.getModuleId());
        if(moduleIds.size()>0) {
            sysPlanItem.getParams().put("moduleIdsOfProject", moduleIds);
        } else {
            sysPlanItem.getParams().put("moduleIdsOfProject", Arrays.asList(0));
        }
        startPage();
        List<SysPlanItem> list = sysPlanItemService.selectSysPlanItemList(sysPlanItem);
        return getDataTable(list);
    }

    /**
     * 查询测试计划子项列表
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @GetMapping("/case/list")
    public TableDataInfo items(SysCase sysCase)
    {
        if(sysCase.getParams()==null) {
            sysCase.setParams(new HashMap<>());
        }
        Set<Long> moduleIds = sysModuleService.getAllChildIds(sysCase.getProjectId(), sysCase.getModuleId());
        if(moduleIds.size()>0) {
            sysCase.getParams().put("moduleIdsOfProject", moduleIds);
        } else {
            sysCase.getParams().put("moduleIdsOfProject", Arrays.asList(0));
        }

        startPage();
        List<SysCase> list = sysPlanItemService.selectCaseList(sysCase);
        return getDataTable(list);
    }

    /**
     * 查询模块列表
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @GetMapping("/module/list")
    public AjaxResult list(SysPlanItemModule sysModule)
    {
        List<SysPlanItemModule> list = sysPlanItemService.selectSysModuleList(sysModule);
        return success(list);
    }

    /**
     * 导出测试计划子项列表
     */
    @PreAuthorize("@ss.hasPermi('system:PlanItem:export')")
    @Log(title = "测试计划子项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysPlanItem sysPlanItem)
    {
        List<SysPlanItem> list = sysPlanItemService.selectSysPlanItemList(sysPlanItem);
        ExcelUtil<SysPlanItem> util = new ExcelUtil<SysPlanItem>(SysPlanItem.class);
        util.exportExcel(response, list, "测试计划子项数据");
    }

    /**
     * 查询指定用例的上一个用例
     * @param sysPlanItem
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @GetMapping(value = "/{planItemId}/prev")
    public AjaxResult selectPrevSysCase(@PathVariable("planItemId") String planItemId, SysPlanItem sysPlanItem) {
        if(sysPlanItem.getParams()==null) {
            sysPlanItem.setParams(new HashMap<>());
        }
        sysPlanItem.getParams().put("prevPlanItemId", planItemId);
        return success(sysPlanItemService.selectPrevSysPlanItem(sysPlanItem));
    }

    /**
     * 查询指定用例的下一个用例
     * @param sysPlanItem
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @GetMapping(value = "/{planItemId}/next")
    public AjaxResult selectNextSysCase(@PathVariable("planItemId") String planItemId, SysPlanItem sysPlanItem) {
        if(sysPlanItem.getParams()==null) {
            sysPlanItem.setParams(new HashMap<>());
        }
        sysPlanItem.getParams().put("nextPlanItemId", planItemId);
        return success(sysPlanItemService.selectNextSysPlanItem(sysPlanItem));
    }

    /**
     * 获取测试计划子项详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @GetMapping(value = "/{planItemId}")
    public AjaxResult getInfo(@PathVariable("planItemId") String planItemId)
    {
        return success(sysPlanItemService.selectSysPlanItemByPlanItemId(planItemId));
    }

    /**
     * 新增测试计划子项
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @Log(title = "测试计划子项", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPlanItem sysPlanItem)
    {
        return toAjax(sysPlanItemService.insertSysPlanItem(sysPlanItem));
    }

    /**
     * 修改测试计划子项
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @Log(title = "测试计划子项", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPlanItem sysPlanItem)
    {
        return toAjax(sysPlanItemService.updateSysPlanItem(sysPlanItem));
    }

    /**
     * 删除测试计划子项
     */
    @PreAuthorize("@ss.hasPermi('system:plan:run') || @ss.hasPermi('system:plan:edit')")
    @Log(title = "测试计划子项", businessType = BusinessType.DELETE)
	@DeleteMapping("/{planItemIds}")
    public AjaxResult remove(@PathVariable String[] planItemIds)
    {
        return toAjax(sysPlanItemService.deleteSysPlanItemByPlanItemIds(planItemIds));
    }
}
