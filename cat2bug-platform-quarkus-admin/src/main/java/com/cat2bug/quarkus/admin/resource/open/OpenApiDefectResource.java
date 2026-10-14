package com.cat2bug.quarkus.admin.resource.open;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.open.ApiDefect;
import com.cat2bug.quarkus.common.domain.open.ApiDefectHandle;
import com.cat2bug.quarkus.common.domain.open.ApiDefectRequest;
import com.cat2bug.quarkus.common.domain.open.type.ApiDefectStateEnum;
import com.cat2bug.quarkus.common.domain.open.type.ApiDefectTypeEnum;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.open.OpenApiContextService;
import com.cat2bug.quarkus.system.open.OpenApiDefectService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;

@Path("/api/defect")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiDefectResource {

    @Inject
    OpenApiContextService openApiContextService;

    @Inject
    OpenApiDefectService openApiDefectService;

    @GET
    public TableDataInfo list(@QueryParam("defectNum") Long defectNum,
                              @QueryParam("defectType") ApiDefectTypeEnum defectType,
                              @QueryParam("defectName") String defectName,
                              @QueryParam("version") String version,
                              @QueryParam("defectState") ApiDefectStateEnum defectState,
                              @QueryParam("defectLevel") String defectLevel,
                              @QueryParam("handlerAccountList") String handlerAccountList,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        Long projectId = openApiContextService.requireProjectId();
        ApiDefectRequest filter = new ApiDefectRequest();
        filter.setDefectNum(defectNum);
        filter.setDefectType(defectType);
        filter.setDefectName(defectName);
        filter.setVersion(version);
        filter.setDefectState(defectState);
        filter.setDefectLevel(defectLevel);
        if (StringUtils.isNotBlank(handlerAccountList)) {
            filter.setHandlerAccountList(Arrays.asList(handlerAccountList.split(",")));
        }
        return openApiDefectService.selectApiDefectPage(projectId, filter, pageNum, pageSize);
    }

    @GET
    @Path("/{defectNumber}")
    public AjaxResult getInfo(@PathParam("defectNumber") Long defectNumber) {
        Long projectId = openApiContextService.requireProjectId();
        return AjaxResult.success(openApiDefectService.selectSysDefectByDefectNumber(projectId, defectNumber));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult add(ApiDefectRequest apiDefect) {
        Long projectId = openApiContextService.requireProjectId();
        return AjaxResult.success(openApiDefectService.insertApiDefect(projectId, apiDefect));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult edit(ApiDefectRequest apiDefect) {
        Long projectId = openApiContextService.requireProjectId();
        if (apiDefect == null) {
            apiDefect = new ApiDefectRequest();
        }
        return AjaxResult.success(openApiDefectService.updateSysDefect(projectId, apiDefect));
    }

    @POST
    @Path("/{defectNum}/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult assign(@PathParam("defectNum") Long defectNum, ApiDefectHandle apiDefectHandle) {
        return AjaxResult.success(workflow(defectNum, apiDefectHandle,
                (projectId, handle) -> openApiDefectService.assign(projectId, handle)));
    }

    @POST
    @Path("/{defectNum}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult reject(@PathParam("defectNum") Long defectNum, ApiDefectHandle apiDefectHandle) {
        return AjaxResult.success(workflow(defectNum, apiDefectHandle,
                (projectId, handle) -> openApiDefectService.reject(projectId, handle)));
    }

    @POST
    @Path("/{defectNum}/repair")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult repair(@PathParam("defectNum") Long defectNum, ApiDefectHandle apiDefectHandle) {
        return AjaxResult.success(workflow(defectNum, apiDefectHandle,
                (projectId, handle) -> openApiDefectService.repair(projectId, handle)));
    }

    @POST
    @Path("/{defectNum}/pass")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult pass(@PathParam("defectNum") Long defectNum, ApiDefectHandle apiDefectHandle) {
        return AjaxResult.success(workflow(defectNum, apiDefectHandle,
                (projectId, handle) -> openApiDefectService.pass(projectId, handle)));
    }

    @POST
    @Path("/{defectNum}/close")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult close(@PathParam("defectNum") Long defectNum, ApiDefectHandle apiDefectHandle) {
        return AjaxResult.success(workflow(defectNum, apiDefectHandle,
                (projectId, handle) -> openApiDefectService.close(projectId, handle)));
    }

    @POST
    @Path("/{defectNum}/open")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult open(@PathParam("defectNum") Long defectNum, ApiDefectHandle apiDefectHandle) {
        return AjaxResult.success(workflow(defectNum, apiDefectHandle,
                (projectId, handle) -> openApiDefectService.open(projectId, handle)));
    }

    private ApiDefect workflow(Long defectNum, ApiDefectHandle apiDefectHandle, WorkflowAction action) {
        Long projectId = openApiContextService.requireProjectId();
        if (apiDefectHandle == null) {
            apiDefectHandle = new ApiDefectHandle();
        }
        apiDefectHandle.setDefectNum(defectNum);
        return action.apply(projectId, apiDefectHandle);
    }

    @FunctionalInterface
    private interface WorkflowAction {
        ApiDefect apply(Long projectId, ApiDefectHandle handle);
    }
}
