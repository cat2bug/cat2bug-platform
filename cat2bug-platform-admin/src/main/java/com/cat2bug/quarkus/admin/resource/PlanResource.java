package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysDefect;
import com.cat2bug.quarkus.common.domain.SysPlan;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysPlanService;
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

@Path("/system/plan")
@Produces(MediaType.APPLICATION_JSON)
public class PlanResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysPlanService sysPlanService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:plan:list")
    public TableDataInfo list(@QueryParam("projectId") Long projectId,
                              @QueryParam("planName") String planName,
                              @QueryParam("planVersion") String planVersion,
                              @QueryParam("planNumber") Long planNumber,
                              @QueryParam("createById") Long createById,
                              @QueryParam("updateById") Long updateById,
                              @QueryParam("reportId") Long reportId,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        query.setPlanName(planName);
        query.setPlanVersion(planVersion);
        query.setPlanNumber(planNumber);
        query.setCreateById(createById);
        query.setUpdateById(updateById);
        query.setReportId(reportId);
        return sysPlanService.selectPlanPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:plan:export")
    public Response export(@RestForm("projectId") Long projectId,
                           @RestForm("planName") String planName,
                           @RestForm("planVersion") String planVersion,
                           @RestForm("planNumber") Long planNumber,
                           @RestForm("createById") Long createById,
                           @RestForm("updateById") Long updateById,
                           @RestForm("reportId") Long reportId) {
        SysPlan query = new SysPlan();
        query.setProjectId(projectId);
        query.setPlanName(planName);
        query.setPlanVersion(planVersion);
        query.setPlanNumber(planNumber);
        query.setCreateById(createById);
        query.setUpdateById(updateById);
        query.setReportId(reportId);
        byte[] workbook = systemExcelExportService.exportPlans(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{planId}")
    @RequiresPermissions("system:plan:query")
    public AjaxResult getInfo(@PathParam("planId") String planId) {
        return AjaxResult.success(sysPlanService.selectSysPlanByPlanId(planId));
    }

    @GET
    @Path("/{planId}/defect/list")
    @RequiresPermissions("system:plan:query")
    public TableDataInfo listDefectOfPlan(@PathParam("planId") String planId,
                                          @QueryParam("projectId") Long projectId,
                                          @QueryParam("moduleId") Long moduleId,
                                          @QueryParam("defectName") String defectName,
                                          @QueryParam("nameVersionKeyword") String nameVersionKeyword,
                                          @QueryParam("defectState") Integer defectState,
                                          @QueryParam("defectType") Integer defectType,
                                          @QueryParam("defectLevel") String defectLevel,
                                          @QueryParam("caseId") Long caseId,
                                          @QueryParam("pageNum") Integer pageNum,
                                          @QueryParam("pageSize") Integer pageSize) {
        SysDefect query = new SysDefect();
        query.setProjectId(projectId);
        query.setModuleId(moduleId);
        query.setDefectName(defectName);
        query.setNameVersionKeyword(nameVersionKeyword);
        query.setDefectState(defectState);
        query.setDefectType(defectType);
        query.setDefectLevel(defectLevel);
        query.setCaseId(caseId);
        return sysPlanService.selectPlanDefectPage(planId, query, pageNum, pageSize);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:plan:add")
    public AjaxResult add(SysPlan sysPlan) {
        return AjaxResult.success(sysPlanService.insertSysPlan(sysPlan));
    }

    @POST
    @Path("/{planId}/copy")
    @RequiresPermissions("system:plan:add")
    public AjaxResult copy(@PathParam("planId") String planId) {
        return toAjax(sysPlanService.copySysPlan(planId));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:plan:edit")
    public AjaxResult edit(SysPlan sysPlan) {
        return toAjax(sysPlanService.updateSysPlan(sysPlan));
    }

    @DELETE
    @Path("/{planIds}")
    @RequiresPermissions("system:plan:remove")
    public AjaxResult remove(@PathParam("planIds") String planIds) {
        String[] ids = Arrays.stream(planIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return toAjax(sysPlanService.deleteSysPlanByPlanIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
