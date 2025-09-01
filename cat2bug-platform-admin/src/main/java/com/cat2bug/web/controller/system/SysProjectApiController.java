package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysProjectApi;
import com.cat2bug.system.service.ISysProjectApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 项目APIController
 * 
 * @author yuzhantao
 * @date 2024-02-11
 */
@RestController
@RequestMapping("/system/api")
public class SysProjectApiController extends BaseController
{
    @Autowired
    private ISysProjectApiService sysProjectApiService;

    /**
     * 查询项目API列表
     */
    @PreAuthorize("@ss.hasPermi('system:api:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysProjectApi sysProjectApi)
    {
        startPage();
        List<SysProjectApi> list = sysProjectApiService.selectSysProjectApiList(sysProjectApi);
        return getDataTable(list);
    }

    /**
     * 导出项目API列表
     */
    @PreAuthorize("@ss.hasPermi('system:api:export')")
    @Log(title = "项目API", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysProjectApi sysProjectApi)
    {
        List<SysProjectApi> list = sysProjectApiService.selectSysProjectApiList(sysProjectApi);
        ExcelUtil<SysProjectApi> util = new ExcelUtil<SysProjectApi>(SysProjectApi.class);
        util.exportExcel(response, list, "项目API数据");
    }

    /**
     * 获取项目API详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:api:query')")
    @GetMapping(value = "/{apiId}")
    public AjaxResult getInfo(@PathVariable("apiId") String apiId)
    {
        return success(sysProjectApiService.selectSysProjectApiByApiId(apiId));
    }

    /**
     * 新增项目API
     */
    @PreAuthorize("@ss.hasPermi('system:api:add')")
    @Log(title = "项目API", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysProjectApi sysProjectApi)
    {
        return toAjax(sysProjectApiService.insertSysProjectApi(sysProjectApi));
    }

    /**
     * 修改项目API
     */
    @PreAuthorize("@ss.hasPermi('system:api:edit')")
    @Log(title = "项目API", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysProjectApi sysProjectApi)
    {
        return toAjax(sysProjectApiService.updateSysProjectApi(sysProjectApi));
    }

    /**
     * 删除项目API
     */
    @PreAuthorize("@ss.hasPermi('system:api:remove')")
    @Log(title = "项目API", businessType = BusinessType.DELETE)
	@DeleteMapping("/{apiIds}")
    public AjaxResult remove(@PathVariable String[] apiIds)
    {
        return toAjax(sysProjectApiService.deleteSysProjectApiByApiIds(apiIds));
    }
}
