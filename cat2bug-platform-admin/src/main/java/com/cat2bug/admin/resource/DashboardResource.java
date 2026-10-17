package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.DefectTrendExcelExportService;
import com.cat2bug.admin.service.PlanStatisticsExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDashboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/system/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    private static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysDashboardService sysDashboardService;

    @Inject
    DefectTrendExcelExportService defectTrendExcelExportService;

    @Inject
    PlanStatisticsExcelExportService planStatisticsExcelExportService;

    @GET
    @Path("/{projectId}/case")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult caseStatistics(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.caseStatistics(projectId));
    }

    @GET
    @Path("/{projectId}/defect")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult defectStatistics(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.defectStatistics(projectId));
    }

    @GET
    @Path("/{projectId}/module")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult moduleStatistics(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.moduleStatistics(projectId));
    }

    @GET
    @Path("/{projectId}/document")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult documentStatistics(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.documentStatistics(projectId));
    }

    @GET
    @Path("/{projectId}/member")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult memberStatistics(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.memberStatistics(projectId));
    }

    @GET
    @Path("/{projectId}/defect-line")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult defectLine(@PathParam("projectId") Long projectId,
                                 @QueryParam("timeType") String timeType) {
        return AjaxResult.success(sysDashboardService.defectLine(projectId, timeType));
    }

    @GET
    @Path("/{projectId}/actions")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult actionList(@PathParam("projectId") Long projectId,
                                 @QueryParam("type") String type) {
        return AjaxResult.success(sysDashboardService.actionList(projectId, type));
    }

    @GET
    @Path("/{projectId}/plans")
    @RequiresPermissions("system:dashboard:query")
    public TableDataInfo planList(@PathParam("projectId") Long projectId) {
        return sysDashboardService.planList(projectId);
    }

    @GET
    @Path("/{projectId}/plan/{planId}")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult planInfo(@PathParam("projectId") Long projectId,
                               @PathParam("planId") String planId) {
        return AjaxResult.success(sysDashboardService.selectPlanByPlanId(planId));
    }

    @GET
    @Path("/{projectId}/plan/{planId}/burndown")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult planBurndown(@PathParam("projectId") Long projectId,
                                   @PathParam("planId") String planId) {
        return AjaxResult.success(sysDashboardService.planBurndown(planId));
    }

    @GET
    @Path("/{projectId}/member-defect")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult memberRankOfDefects(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDashboardService.memberRankOfDefects(projectId));
    }

    @GET
    @Path("/{projectId}/member-defect-line")
    @RequiresPermissions("system:dashboard:query")
    public AjaxResult memberOfDefectsLine(@PathParam("projectId") Long projectId,
                                          @QueryParam("timeType") String timeType) {
        return AjaxResult.success(sysDashboardService.memberOfDefectsLine(projectId, timeType));
    }

    @POST
    @Path("/{projectId}/defect-line/export")
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:dashboard:query")
    public Response exportDefectLine(@PathParam("projectId") Long projectId,
                                     @QueryParam("timeType") String timeType) {
        byte[] workbook = defectTrendExcelExportService.exportDefectStateLine(projectId, timeType);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @POST
    @Path("/{projectId}/member-defect-line/export")
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:dashboard:query")
    public Response exportMemberDefectLine(@PathParam("projectId") Long projectId,
                                           @QueryParam("timeType") String timeType) {
        byte[] workbook = defectTrendExcelExportService.exportMemberDefectLine(projectId, timeType);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @POST
    @Path("/{projectId}/plan-statistics/export")
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:dashboard:query")
    public Response exportPlanStatistics(@PathParam("projectId") Long projectId) {
        byte[] workbook = planStatisticsExcelExportService.exportPlanStatistics(projectId);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }
}
