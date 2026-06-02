package com.cat2bug.web.controller.system;

import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.system.domain.SysDefectOpenWorkload;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.system.service.ISysDefectStatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.web.service.DefectTrendExcelExport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
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
    @Autowired
    private DefectTrendExcelExport defectTrendExcelExport;
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

    /**
     * 团队未关闭待办负载（分页，含全部项目成员）
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/open-workload/{projectId}")
    public TableDataInfo getOpenWorkload(@PathVariable("projectId") Long projectId)
    {
        startPage();
        List<SysDefectOpenWorkload> list = sysDefectStatisticService.openWorkload(projectId);
        return getDataTable(list);
    }

    /**
     * 我的未关闭待办汇总
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/open-workload/{projectId}/my")
    public AjaxResult getMyOpenWorkload(@PathVariable("projectId") Long projectId)
    {
        return success(sysDefectStatisticService.openWorkloadMy(projectId, getUserId()));
    }

    /**
     * 我的缺陷日志参与热力（按日）
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/participation/{projectId}/my")
    public AjaxResult getMyParticipation(@PathVariable("projectId") Long projectId,
                                         @RequestParam(value = "days", defaultValue = "30") int days)
    {
        return success(sysDefectStatisticService.participationMy(projectId, getUserId(), days));
    }

    /**
     * 项目测试计划列表（轻量）
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/plan/list")
    public AjaxResult getPlanList(@RequestParam("projectId") Long projectId)
    {
        return success(sysDefectStatisticService.planList(projectId));
    }

    /**
     * 测试计划燃尽图
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/plan/{planId}/burndown")
    public AjaxResult getPlanBurndown(@PathVariable("planId") String planId)
    {
        return success(sysDefectStatisticService.planBurndownChart(planId));
    }

    /**
     * 项目测试计划质量指标
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/plan-metrics/{projectId}")
    public AjaxResult getPlanMetrics(@PathVariable("projectId") Long projectId)
    {
        return success(sysDefectStatisticService.planMetrics(projectId));
    }

    /**
     * 测试计划倒计时摘要
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/plan/{planId}/summary")
    public AjaxResult getPlanCountdownSummary(@PathVariable("planId") String planId)
    {
        return success(sysDefectStatisticService.planCountdownSummary(planId));
    }

    /**
     * 缺陷状态走势（与仪表盘 defect-line 口径一致）
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/defect-state-line/{projectId}")
    public AjaxResult getDefectStateLine(@PathVariable("projectId") Long projectId,
                                         @RequestParam("timeType") String timeType)
    {
        return success(sysDefectStatisticService.defectStateLine(projectId, timeType));
    }

    /**
     * 成员处理缺陷走势（与仪表盘 member-defect-line 口径一致）
     */
    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @GetMapping(value = "/member-defect-line/{projectId}")
    public AjaxResult getMemberDefectLine(@PathVariable("projectId") Long projectId,
                                          @RequestParam("timeType") String timeType)
    {
        return success(sysDefectStatisticService.memberDefectLine(projectId, timeType));
    }

    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @Log(title = "缺陷", businessType = BusinessType.EXPORT)
    @PostMapping(value = "/defect-state-line/{projectId}/export")
    public void exportDefectStateLine(HttpServletResponse response,
                                      @PathVariable("projectId") Long projectId,
                                      @RequestParam("timeType") String timeType) throws Exception
    {
        defectTrendExcelExport.exportDefectStateLine(response, projectId, timeType);
    }

    @PreAuthorize("@ss.hasPermi('system:defect:query')")
    @Log(title = "缺陷", businessType = BusinessType.EXPORT)
    @PostMapping(value = "/member-defect-line/{projectId}/export")
    public void exportMemberDefectLine(HttpServletResponse response,
                                       @PathVariable("projectId") Long projectId,
                                       @RequestParam("timeType") String timeType) throws Exception
    {
        defectTrendExcelExport.exportMemberDefectLine(response, projectId, timeType);
    }
}
