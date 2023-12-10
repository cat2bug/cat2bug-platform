package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysScreenSize;
import com.cat2bug.system.service.ISysScreenSizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 屏幕尺寸Controller
 * 
 * @author yuzhantao
 * @date 2023-12-10
 */
@RestController
@RequestMapping("/system/ScreenSize")
public class SysScreenSizeController extends BaseController
{
    @Autowired
    private ISysScreenSizeService sysScreenSizeService;

    /**
     * 查询屏幕尺寸列表
     */
    @PreAuthorize("@ss.hasPermi('system:ScreenSize:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysScreenSize sysScreenSize)
    {
        startPage();
        List<SysScreenSize> list = sysScreenSizeService.selectSysScreenSizeList(sysScreenSize);
        return getDataTable(list);
    }

    /**
     * 导出屏幕尺寸列表
     */
    @PreAuthorize("@ss.hasPermi('system:ScreenSize:export')")
    @Log(title = "屏幕尺寸", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysScreenSize sysScreenSize)
    {
        List<SysScreenSize> list = sysScreenSizeService.selectSysScreenSizeList(sysScreenSize);
        ExcelUtil<SysScreenSize> util = new ExcelUtil<SysScreenSize>(SysScreenSize.class);
        util.exportExcel(response, list, "屏幕尺寸数据");
    }

    /**
     * 获取屏幕尺寸详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:ScreenSize:query')")
    @GetMapping(value = "/{screenSizeId}")
    public AjaxResult getInfo(@PathVariable("screenSizeId") Long screenSizeId)
    {
        return success(sysScreenSizeService.selectSysScreenSizeByScreenSizeId(screenSizeId));
    }

    /**
     * 新增屏幕尺寸
     */
    @PreAuthorize("@ss.hasPermi('system:ScreenSize:add')")
    @Log(title = "屏幕尺寸", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysScreenSize sysScreenSize)
    {
        return toAjax(sysScreenSizeService.insertSysScreenSize(sysScreenSize));
    }

    /**
     * 修改屏幕尺寸
     */
    @PreAuthorize("@ss.hasPermi('system:ScreenSize:edit')")
    @Log(title = "屏幕尺寸", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysScreenSize sysScreenSize)
    {
        return toAjax(sysScreenSizeService.updateSysScreenSize(sysScreenSize));
    }

    /**
     * 删除屏幕尺寸
     */
    @PreAuthorize("@ss.hasPermi('system:ScreenSize:remove')")
    @Log(title = "屏幕尺寸", businessType = BusinessType.DELETE)
	@DeleteMapping("/{screenSizeIds}")
    public AjaxResult remove(@PathVariable Long[] screenSizeIds)
    {
        return toAjax(sysScreenSizeService.deleteSysScreenSizeByScreenSizeIds(screenSizeIds));
    }
}
