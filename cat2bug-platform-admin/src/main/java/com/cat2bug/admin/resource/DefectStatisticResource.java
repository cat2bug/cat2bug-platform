package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.DefectTrendExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDefectStatisticService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/system/defect/statistic")
@Produces(MediaType.APPLICATION_JSON)
public class DefectStatisticResource {

    private static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysDefectStatisticService sysDefectStatisticService;

    @Inject
    DefectTrendExcelExportService defectTrendExcelExportService;

    @Inject
    AuthContext authContext;

    @GET
    @Path("/type/{projectId}")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getTypeStatistic(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectStatisticService.typeStatistic(projectId, null));
    }

    @GET
    @Path("/type/{projectId}/my")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getMyTypeStatistic(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectStatisticService.typeStatistic(projectId, requireUserId()));
    }

    @GET
    @Path("/state/{projectId}")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getStateStatistic(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectStatisticService.stateStatistic(projectId, null));
    }

    @GET
    @Path("/state/{projectId}/my")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getMyStateStatistic(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectStatisticService.stateStatistic(projectId, requireUserId()));
    }

    @GET
    @Path("/module/{projectId}")
    @RequiresPermissions("system:defect:query")
    public TableDataInfo getModuleStatistic(@PathParam("projectId") Long projectId,
                                            @QueryParam("pageNum") Integer pageNum,
                                            @QueryParam("pageSize") Integer pageSize) {
        return sysDefectStatisticService.moduleStatisticPage(projectId, pageNum, pageSize);
    }

    @GET
    @Path("/open-workload/{projectId}")
    @RequiresPermissions("system:defect:query")
    public TableDataInfo getOpenWorkload(@PathParam("projectId") Long projectId,
                                         @QueryParam("pageNum") Integer pageNum,
                                         @QueryParam("pageSize") Integer pageSize) {
        return sysDefectStatisticService.openWorkloadPage(projectId, pageNum, pageSize);
    }

    @GET
    @Path("/open-workload/{projectId}/my")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getMyOpenWorkload(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectStatisticService.openWorkloadMy(projectId, requireUserId()));
    }

    @GET
    @Path("/participation/{projectId}/my")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getMyParticipation(@PathParam("projectId") Long projectId,
                                         @QueryParam("days") Integer days) {
        int windowDays = days != null ? days : 30;
        return AjaxResult.success(sysDefectStatisticService.participationMy(
                projectId, requireUserId(), windowDays));
    }

    @GET
    @Path("/plan/list")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getPlanList(@QueryParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectStatisticService.planList(projectId));
    }

    @GET
    @Path("/plan/{planId}/burndown")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getPlanBurndown(@PathParam("planId") String planId) {
        return AjaxResult.success(sysDefectStatisticService.planBurndownChart(planId));
    }

    @GET
    @Path("/plan-metrics/{projectId}")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getPlanMetrics(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysDefectStatisticService.planMetrics(projectId));
    }

    @GET
    @Path("/plan/{planId}/summary")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getPlanCountdownSummary(@PathParam("planId") String planId) {
        return AjaxResult.success(sysDefectStatisticService.planCountdownSummary(planId));
    }

    @GET
    @Path("/defect-state-line/{projectId}")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getDefectStateLine(@PathParam("projectId") Long projectId,
                                         @QueryParam("timeType") String timeType) {
        return AjaxResult.success(sysDefectStatisticService.defectStateLine(projectId, timeType));
    }

    @GET
    @Path("/member-defect-line/{projectId}")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getMemberDefectLine(@PathParam("projectId") Long projectId,
                                          @QueryParam("timeType") String timeType) {
        return AjaxResult.success(sysDefectStatisticService.memberDefectLine(projectId, timeType));
    }

    @POST
    @Path("/defect-state-line/{projectId}/export")
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:defect:query")
    public Response exportDefectStateLine(@PathParam("projectId") Long projectId,
                                          @QueryParam("timeType") String timeType) {
        byte[] workbook = defectTrendExcelExportService.exportDefectStateLine(projectId, timeType);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @POST
    @Path("/member-defect-line/{projectId}/export")
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:defect:query")
    public Response exportMemberDefectLine(@PathParam("projectId") Long projectId,
                                           @QueryParam("timeType") String timeType) {
        byte[] workbook = defectTrendExcelExportService.exportMemberDefectLine(projectId, timeType);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    private Long requireUserId() {
        LoginUser loginUser = authContext.getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }
}
