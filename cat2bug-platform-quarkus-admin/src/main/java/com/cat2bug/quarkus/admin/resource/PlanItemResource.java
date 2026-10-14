package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysCase;
import com.cat2bug.quarkus.common.domain.SysPlanItem;
import com.cat2bug.quarkus.common.domain.SysPlanItemModule;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysPlanItemService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.util.Arrays;
import java.util.HashMap;

@Path("/system/PlanItem")
@Produces(MediaType.APPLICATION_JSON)
public class PlanItemResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysPlanItemService sysPlanItemService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public TableDataInfo list(@QueryParam("planId") String planId,
                              @QueryParam("projectId") Long projectId,
                              @QueryParam("moduleId") Long moduleId,
                              @QueryParam("caseId") Long caseId,
                              @QueryParam("planItemState") String planItemState,
                              @QueryParam("updateById") Long updateById,
                              @QueryParam("caseLevel") Long caseLevel,
                              @QueryParam("params[beginUpdateTime]") String beginUpdateTime,
                              @QueryParam("params[endUpdateTime]") String endUpdateTime,
                              @QueryParam("params[caseName]") String caseName,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysPlanItem query = new SysPlanItem();
        query.setPlanId(planId);
        query.setProjectId(projectId);
        query.setModuleId(moduleId);
        query.setCaseId(caseId);
        query.setPlanItemState(planItemState);
        query.setUpdateById(updateById);
        query.setCaseLevel(caseLevel);
        query.setParams(new HashMap<>());
        if (beginUpdateTime != null) {
            query.getParams().put("beginUpdateTime", beginUpdateTime);
        }
        if (endUpdateTime != null) {
            query.getParams().put("endUpdateTime", endUpdateTime);
        }
        if (caseName != null) {
            query.getParams().put("caseName", caseName);
        }
        return sysPlanItemService.selectPlanItemPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:PlanItem:export")
    public Response export(@RestForm("planId") String planId,
                           @RestForm("projectId") Long projectId,
                           @RestForm("moduleId") Long moduleId,
                           @RestForm("caseId") Long caseId,
                           @RestForm("planItemState") String planItemState,
                           @RestForm("updateById") Long updateById,
                           @RestForm("caseLevel") Long caseLevel) {
        SysPlanItem query = new SysPlanItem();
        query.setPlanId(planId);
        query.setProjectId(projectId);
        query.setModuleId(moduleId);
        query.setCaseId(caseId);
        query.setPlanItemState(planItemState);
        query.setUpdateById(updateById);
        query.setCaseLevel(caseLevel);
        query.setParams(new HashMap<>());
        byte[] workbook = systemExcelExportService.exportPlanItems(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/case/list")
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public TableDataInfo caseList(@QueryParam("projectId") Long projectId,
                                  @QueryParam("moduleId") Long moduleId,
                                  @QueryParam("caseName") String caseName,
                                  @QueryParam("caseType") Long caseType,
                                  @QueryParam("caseLevel") Long caseLevel,
                                  @QueryParam("params[planId]") String planId,
                                  @QueryParam("pageNum") Integer pageNum,
                                  @QueryParam("pageSize") Integer pageSize) {
        SysCase query = new SysCase();
        query.setProjectId(projectId);
        query.setModuleId(moduleId);
        query.setCaseName(caseName);
        query.setCaseType(caseType);
        query.setCaseLevel(caseLevel);
        query.setParams(new HashMap<>());
        if (planId != null) {
            query.getParams().put("planId", planId);
        }
        return sysPlanItemService.selectCasePage(query, pageNum, pageSize);
    }

    @GET
    @Path("/module/list")
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult moduleList(@QueryParam("modulePid") Long modulePid,
                                 @QueryParam("moduleName") String moduleName,
                                 @QueryParam("projectId") Long projectId,
                                 @QueryParam("planId") String planId) {
        SysPlanItemModule query = new SysPlanItemModule();
        query.setModulePid(modulePid);
        query.setModuleName(moduleName);
        query.setProjectId(projectId);
        query.setPlanId(planId);
        return AjaxResult.success(sysPlanItemService.selectSysModuleList(query));
    }

    @GET
    @Path("/{planItemId}/prev")
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult prev(@PathParam("planItemId") String planItemId,
                           @QueryParam("planId") String planId,
                           @QueryParam("projectId") Long projectId,
                           @QueryParam("moduleId") Long moduleId) {
        SysPlanItem query = buildNavQuery(planId, projectId, moduleId);
        return AjaxResult.success(sysPlanItemService.selectPrevSysPlanItem(query, planItemId));
    }

    @GET
    @Path("/{planItemId}/next")
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult next(@PathParam("planItemId") String planItemId,
                           @QueryParam("planId") String planId,
                           @QueryParam("projectId") Long projectId,
                           @QueryParam("moduleId") Long moduleId) {
        SysPlanItem query = buildNavQuery(planId, projectId, moduleId);
        return AjaxResult.success(sysPlanItemService.selectNextSysPlanItem(query, planItemId));
    }

    @GET
    @Path("/{planItemId}")
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult getInfo(@PathParam("planItemId") String planItemId) {
        return AjaxResult.success(sysPlanItemService.selectSysPlanItemByPlanItemId(planItemId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult add(SysPlanItem sysPlanItem) {
        return toAjax(sysPlanItemService.insertSysPlanItem(sysPlanItem));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult edit(SysPlanItem sysPlanItem) {
        return toAjax(sysPlanItemService.updateSysPlanItem(sysPlanItem));
    }

    @DELETE
    @Path("/{planItemIds}")
    @RequiresPermissions(value = {"system:plan:run", "system:plan:edit"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult remove(@PathParam("planItemIds") String planItemIds) {
        String[] ids = Arrays.stream(planItemIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return toAjax(sysPlanItemService.deleteSysPlanItemByPlanItemIds(ids));
    }

    private SysPlanItem buildNavQuery(String planId, Long projectId, Long moduleId) {
        SysPlanItem query = new SysPlanItem();
        query.setPlanId(planId);
        query.setProjectId(projectId);
        query.setModuleId(moduleId);
        query.setParams(new HashMap<>());
        return query;
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
