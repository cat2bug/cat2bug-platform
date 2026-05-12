package com.cat2bug.api.controller;

import com.cat2bug.api.domain.ApiDeliverable;
import com.cat2bug.api.domain.ApiDeliverableCreateRequest;
import com.cat2bug.api.service.ApiService;
import com.cat2bug.api.service.IApiDeliverableService;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.page.TableDataInfo;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysModule;
import com.cat2bug.system.service.ISysModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    @Resource
    private ApiService apiService;

    @Autowired
    private ISysModuleService sysModuleService;

    /**
     * 查询交付物列表（子级列表）。父级由全路径 {@code parentDeliverablePath} 指定，不传表示根节点下的一级交付物。
     */
    @GetMapping
    public TableDataInfo list(@RequestParam(value = "parentDeliverablePath", required = false) String parentDeliverablePath)
    {
        Long projectId = this.apiService.getProjectId();
        long parentPid = this.apiDeliverableService.resolveParentModulePid(projectId, parentDeliverablePath);
        if (parentPid < 0) {
            startPage();
            return getDataTable(Collections.emptyList());
        }
        ApiDeliverable filter = new ApiDeliverable();
        filter.setDeliverablePid(parentPid);
        startPage();
        List<ApiDeliverable> list = this.apiDeliverableService.selectApiDeliverableList(filter);
        return getDataTable(list);
    }

    /**
     * 当前项目全部交付物（扁平全路径），含各节点直接子数量；一次返回便于 MCP/客户端整树展开展示。
     */
    @GetMapping("/tree")
    public AjaxResult tree() {
        Long projectId = this.apiService.getProjectId();
        List<ApiDeliverable> list = this.apiDeliverableService.selectApiDeliverableTreeFlat(projectId);
        return AjaxResult.success(list);
    }

    /**
     * 查询交付物详情（按全路径或节点名称，与创建/筛选时约定一致）
     */
    @GetMapping("/info")
    public AjaxResult getInfo(@RequestParam("deliverablePath") String deliverablePath)
    {
        if (StringUtils.isBlank(deliverablePath)) {
            return AjaxResult.error("deliverablePath 不能为空");
        }
        Long projectId = this.apiService.getProjectId();
        Optional<ApiDeliverable> view = this.apiDeliverableService.buildApiViewByPathOrName(projectId, deliverablePath.trim());
        return view.map(AjaxResult::success).orElseGet(() -> AjaxResult.error("交付物不存在或无权限访问"));
    }

    /**
     * 新增交付物
     */
    @PostMapping
    public AjaxResult add(@RequestBody ApiDeliverableCreateRequest body)
    {
        if (StringUtils.isBlank(body.getDeliverableName())) {
            return AjaxResult.error("deliverableName 为必填");
        }
        Long projectId = this.apiService.getProjectId();
        long parentPid = this.apiDeliverableService.resolveParentModulePid(projectId, body.getParentDeliverablePath());
        if (parentPid < 0) {
            return AjaxResult.error("父级交付物不存在: " + body.getParentDeliverablePath());
        }
        SysModule sysModule = new SysModule();
        sysModule.setModuleName(body.getDeliverableName().trim());
        sysModule.setRemark(body.getRemark());
        sysModule.setModulePid(parentPid);
        sysModule.setProjectId(projectId);
        List<SysModule> created = this.sysModuleService.insertSysModule(sysModule);
        if (created.isEmpty()) {
            return AjaxResult.error("创建失败");
        }
        SysModule first = created.get(0);
        String fullPath = this.apiDeliverableService.selectApiDeliverablePathList(projectId).stream()
                .filter(p -> first.getModuleId().equals(p.getDeliverableId()))
                .map(ApiDeliverable::getDeliverablePath)
                .findFirst()
                .orElse(first.getModuleName());
        Optional<ApiDeliverable> view = this.apiDeliverableService.buildApiViewByPathOrName(projectId, fullPath);
        if (view.isPresent()) {
            return AjaxResult.success(view.get());
        }
        ApiDeliverable d = new ApiDeliverable();
        d.setDeliverableName(first.getModuleName());
        d.setDeliverablePath(first.getModuleName());
        d.setRemark(first.getRemark());
        d.setChildrenCount(0);
        return AjaxResult.success(d);
    }
}
