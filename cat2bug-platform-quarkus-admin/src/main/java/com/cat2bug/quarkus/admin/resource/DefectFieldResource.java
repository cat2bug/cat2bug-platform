package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.SysProjectDefectField;
import com.cat2bug.quarkus.common.domain.SysProjectDefectFieldManageItem;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysProjectDefectFieldService;
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

    @Inject
    SysProjectDefectFieldService defectFieldService;

    @GET
    @RequiresPermissions("system:project:defect-field:list")
    public AjaxResult list(@PathParam("projectId") Long projectId) {
        List<SysProjectDefectFieldManageItem> list = defectFieldService.selectManageListByProjectId(projectId);
        return AjaxResult.success(list);
    }

    @PUT
    @Path("/builtin-layout")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:defect-field:edit")
    public AjaxResult updateBuiltinLayout(
            @PathParam("projectId") Long projectId,
            List<SysProjectDefectFieldManageItem> layout) {
        return toAjax(defectFieldService.updateBuiltinFieldLayout(projectId, layout));
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
        field.setProjectId(projectId);
        return AjaxResult.success(defectFieldService.insert(field));
    }

    @PUT
    @Path("/{fieldId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:defect-field:edit")
    public AjaxResult edit(
            @PathParam("projectId") Long projectId,
            @PathParam("fieldId") Long fieldId,
            SysProjectDefectField field) {
        field.setProjectId(projectId);
        field.setFieldId(fieldId);
        return toAjax(defectFieldService.update(field));
    }

    @DELETE
    @Path("/{fieldId}")
    @RequiresPermissions("system:project:defect-field:remove")
    public AjaxResult remove(@PathParam("projectId") Long projectId, @PathParam("fieldId") Long fieldId) {
        return toAjax(defectFieldService.softDelete(projectId, fieldId));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
