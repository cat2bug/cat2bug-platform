package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.service.ISysPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 测试计划Controller
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
@RestController
@RequestMapping("/system/plan")
public class SysPlanController extends BaseController
{
    @Autowired
    private ISysPlanService sysPlanService;

    /**
     * 查询测试计划列表
     */
    @PreAuthorize("@ss.hasPermi('system:plan:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysPlan sysPlan)
    {
        startPage();
        List<SysPlan> list = sysPlanService.selectSysPlanList(sysPlan);
        return getDataTable(list);
    }

    /**
     * 导出测试计划列表
     */
    @PreAuthorize("@ss.hasPermi('system:plan:export')")
    @Log(title = "测试计划", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysPlan sysPlan)
    {
        List<SysPlan> list = sysPlanService.selectSysPlanList(sysPlan);
        ExcelUtil<SysPlan> util = new ExcelUtil<SysPlan>(SysPlan.class);
        util.exportExcel(response, list, "测试计划数据");
    }

    /**
     * 获取测试计划详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:plan:query')")
    @GetMapping(value = "/{planId}")
    public AjaxResult getInfo(@PathVariable("planId") String planId)
    {
        return success(sysPlanService.selectSysPlanByPlanId(planId));
    }

    /**
     * 新增测试计划
     */
    @PreAuthorize("@ss.hasPermi('system:plan:add')")
    @Log(title = "测试计划", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPlan sysPlan)
    {
        return toAjax(sysPlanService.insertSysPlan(sysPlan));
    }

    /**
     * 修改测试计划
     */
    @PreAuthorize("@ss.hasPermi('system:plan:edit')")
    @Log(title = "测试计划", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPlan sysPlan)
    {
        return toAjax(sysPlanService.updateSysPlan(sysPlan));
    }

    /**
     * 删除测试计划
     */
    @PreAuthorize("@ss.hasPermi('system:plan:remove')")
    @Log(title = "测试计划", businessType = BusinessType.DELETE)
	@DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable String[] planIds)
    {
        return toAjax(sysPlanService.deleteSysPlanByPlanIds(planIds));
    }
}