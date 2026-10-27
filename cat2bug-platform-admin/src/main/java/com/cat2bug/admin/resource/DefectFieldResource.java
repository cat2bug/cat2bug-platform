package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.domain.SysProjectDefectFieldManageItem;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysProjectDefectFieldService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/system/project/{projectId}/defect-fields")
@Produces(MediaType.APPLICATION_JSON)
public class DefectFieldResource {

    private static final int HTTP_GONE = 410;
    private static final String MIGRATED_MSG =
            "缺陷属性已迁至团队设置，请使用 /system/team/{teamId}/defect-fields";

    @Inject
    SysProjectDefectFieldService defectFieldService;

    @GET
    @RequiresPermissions("system:project:defect-field:list")
    public AjaxResult list(@PathParam("projectId") Long projectId) {
        return rejectProjectWrite();
    }

    @PUT
    @Path("/builtin-layout")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:defect-field:edit")
    public AjaxResult updateBuiltinLayout(
            @PathParam("projectId") Long projectId,
            List<SysProjectDefectFieldManageItem> layout) {
        return rejectProjectWrite();
    }

    @GET
    @Path("/enabled")
    @RequiresPermissions("system:defect:list")
    public AjaxResult enabledList(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(defectFieldService.selectEnabledListByProjectId(projectId));
    }

    @GET
    @Path("/column-layout")
    @RequiresPermissions("system:defect:list")
    public AjaxResult columnLayout(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(defectFieldService.selectColumnLayoutByProjectId(projectId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:defect-field:add")
    public AjaxResult add(@PathParam("projectId") Long projectId, SysProjectDefectField field) {
        return rejectProjectWrite();
    }

    @PUT
    @Path("/{fieldId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:defect-field:edit")
    public AjaxResult edit(
            @PathParam("projectId") Long projectId,
            @PathParam("fieldId") Long fieldId,
            SysProjectDefectField field) {
        return rejectProjectWrite();
    }

    @DELETE
    @Path("/{fieldId}")
    @RequiresPermissions("system:project:defect-field:remove")
    public AjaxResult remove(@PathParam("projectId") Long projectId, @PathParam("fieldId") Long fieldId) {
        return rejectProjectWrite();
    }

    private static AjaxResult rejectProjectWrite() {
        throw new ApiException(HTTP_GONE, MIGRATED_MSG);
    }
}
