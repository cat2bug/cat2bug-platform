package com.cat2bug.web.controller.system;

import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysDictData;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.system.service.ISysDefectStatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-01-22 01:54
 * @Version: 1.0.0
 * 缺陷统计
 */
@RestController
@RequestMapping("/system/defect/statistic")
public class SysDefectStatisticController extends BaseController {
    @Autowired
    private ISysDefectStatisticService sysDefectStatisticService;
    /**
     * 获取缺陷分类统计
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/type/{projectId}")
    public AjaxResult getTypeStatistic(@PathVariable("projectId") Long projectId)
    {
        return success(sysDefectStatisticService.typeStatistic(projectId,null));
    }
    /**
     * 获取我的缺陷分类统计
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/type/{projectId}/my")
    public AjaxResult getMyTypeStatistic(@PathVariable("projectId") Long projectId)
    {
        return success(sysDefectStatisticService.typeStatistic(projectId,getUserId()));
    }

    /**
     * 获取缺陷分类统计
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/state/{projectId}")
    public AjaxResult getStateStatistic(@PathVariable("projectId") Long projectId)
    {
        return success(sysDefectStatisticService.stateStatistic(projectId,null));
    }

    /**
     * 获取我的缺陷分类统计
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/state/{projectId}/my")
    public AjaxResult getMyStateStatistic(@PathVariable("projectId") Long projectId)
    {
        return success(sysDefectStatisticService.stateStatistic(projectId,getUserId()));
    }

    /**
     * 获取模块统计
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/module/{projectId}")
    public TableDataInfo getModuleStatistic(@PathVariable("projectId") Long projectId)
    {
        startPage();
        List<Map<String, Object>> list = sysDefectStatisticService.moduleStatistic(projectId);
        return getDataTable(list);
    }
}
