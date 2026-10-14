package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysProjectApi;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysProjectApiService;
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

@Path("/system/api")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectApiResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysProjectApiService sysProjectApiService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:api:list")
    public TableDataInfo list(@QueryParam("projectId") Long projectId,
                              @QueryParam("userId") Long userId,
                              @QueryParam("apiName") String apiName,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysProjectApi query = new SysProjectApi();
        query.setProjectId(projectId);
        query.setUserId(userId);
        query.setApiName(apiName);
        return sysProjectApiService.selectProjectApiPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:api:export")
    public Response export(@RestForm("projectId") Long projectId,
                           @RestForm("userId") Long userId,
                           @RestForm("apiName") String apiName) {
        SysProjectApi query = new SysProjectApi();
        query.setProjectId(projectId);
        query.setUserId(userId);
        query.setApiName(apiName);
        byte[] workbook = systemExcelExportService.exportProjectApis(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{apiId}")
    @RequiresPermissions("system:api:query")
    public AjaxResult getInfo(@PathParam("apiId") String apiId) {
        return AjaxResult.success(sysProjectApiService.selectSysProjectApiByApiId(apiId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:api:add")
    public AjaxResult add(SysProjectApi sysProjectApi) {
        return toAjax(sysProjectApiService.insertSysProjectApi(sysProjectApi));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:api:edit")
    public AjaxResult edit(SysProjectApi sysProjectApi) {
        return toAjax(sysProjectApiService.updateSysProjectApi(sysProjectApi));
    }

    @DELETE
    @Path("/{apiIds}")
    @RequiresPermissions("system:api:remove")
    public AjaxResult remove(@PathParam("apiIds") String apiIds) {
        String[] ids = Arrays.stream(apiIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return toAjax(sysProjectApiService.deleteSysProjectApiByApiIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
