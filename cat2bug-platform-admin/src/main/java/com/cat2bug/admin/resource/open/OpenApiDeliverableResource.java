package com.cat2bug.admin.resource.open;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysModule;
import com.cat2bug.common.domain.open.ApiDeliverable;
import com.cat2bug.common.domain.open.ApiDeliverableCreateRequest;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.open.OpenApiContextService;
import com.cat2bug.system.open.OpenApiDeliverableService;
import com.cat2bug.system.service.SysModuleService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Path("/api/deliverable")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiDeliverableResource {

    @Inject
    OpenApiContextService openApiContextService;

    @Inject
    OpenApiDeliverableService openApiDeliverableService;

    @Inject
    SysModuleService sysModuleService;

    /**
     * 查询交付物列表（子级列表）。父级由全路径 {@code parentDeliverablePath} 指定，不传表示根节点下的一级交付物。
     */
    @GET
    public TableDataInfo list(@QueryParam("parentDeliverablePath") String parentDeliverablePath) {
        Long projectId = openApiContextService.requireProjectId();
        long parentPid = openApiDeliverableService.resolveParentModulePid(projectId, parentDeliverablePath);
        if (parentPid < 0) {
            return new TableDataInfo(Collections.emptyList(), 0);
        }
        ApiDeliverable filter = new ApiDeliverable();
        filter.setDeliverablePid(parentPid);
        List<ApiDeliverable> rows = openApiDeliverableService.selectApiDeliverableList(projectId, filter);
        return new TableDataInfo(rows, rows.size());
    }

    /**
     * 当前项目全部交付物（扁平全路径），含各节点直接子数量。
     */
    @GET
    @Path("/tree")
    public AjaxResult tree() {
        Long projectId = openApiContextService.requireProjectId();
        List<ApiDeliverable> list = openApiDeliverableService.selectApiDeliverableTreeFlat(projectId);
        return AjaxResult.success(list);
    }

    /**
     * 查询交付物详情（按全路径或节点名称）
     */
    @GET
    @Path("/info")
    public AjaxResult getInfo(@QueryParam("deliverablePath") String deliverablePath) {
        if (StringUtils.isBlank(deliverablePath)) {
            return AjaxResult.error("deliverablePath 不能为空");
        }
        Long projectId = openApiContextService.requireProjectId();
        Optional<ApiDeliverable> view = openApiDeliverableService.buildApiViewByPathOrName(
                projectId, deliverablePath.trim());
        return view.map(AjaxResult::success)
                .orElseGet(() -> AjaxResult.error("交付物不存在或无权限访问"));
    }

    /**
     * 新增交付物
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult add(ApiDeliverableCreateRequest body) {
        if (body == null || StringUtils.isBlank(body.getDeliverableName())) {
            return AjaxResult.error("deliverableName 为必填");
        }
        Long projectId = openApiContextService.requireProjectId();
        long parentPid = openApiDeliverableService.resolveParentModulePid(projectId, body.getParentDeliverablePath());
        if (parentPid < 0) {
            return AjaxResult.error("父级交付物不存在: " + body.getParentDeliverablePath());
        }
        SysModule sysModule = new SysModule();
        sysModule.setModuleName(body.getDeliverableName().trim());
        sysModule.setRemark(body.getRemark());
        sysModule.setModulePid(parentPid);
        sysModule.setProjectId(projectId);
        List<SysModule> created = sysModuleService.insertSysModule(sysModule);
        if (created.isEmpty()) {
            return AjaxResult.error("创建失败");
        }
        SysModule first = created.get(0);
        String fullPath = openApiDeliverableService.selectApiDeliverablePathList(projectId).stream()
                .filter(p -> first.getModuleId().equals(p.getDeliverableId()))
                .map(ApiDeliverable::getDeliverablePath)
                .findFirst()
                .orElse(first.getModuleName());
        Optional<ApiDeliverable> view = openApiDeliverableService.buildApiViewByPathOrName(projectId, fullPath);
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
