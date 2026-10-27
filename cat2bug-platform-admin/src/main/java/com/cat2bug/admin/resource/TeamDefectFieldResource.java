package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.domain.SysProjectDefectFieldManageItem;
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

@Path("/system/team/{teamId}/defect-fields")
@Produces(MediaType.APPLICATION_JSON)
public class TeamDefectFieldResource {

    @Inject
    SysProjectDefectFieldService defectFieldService;

    @GET
    @RequiresPermissions("system:team:defect-field:list")
    public AjaxResult list(@PathParam("teamId") Long teamId) {
        List<SysProjectDefectFieldManageItem> list = defectFieldService.selectManageListByTeamId(teamId);
        return AjaxResult.success(list);
    }

    @PUT
    @Path("/builtin-layout")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:defect-field:edit")
    public AjaxResult updateBuiltinLayout(
            @PathParam("teamId") Long teamId,
            List<SysProjectDefectFieldManageItem> layout) {
        return toAjax(defectFieldService.updateBuiltinFieldLayout(teamId, layout));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:defect-field:add")
    public AjaxResult add(@PathParam("teamId") Long teamId, SysProjectDefectField field) {
        field.setTeamId(teamId);
        return AjaxResult.success(defectFieldService.insert(field));
    }

    @PUT
    @Path("/{fieldId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:defect-field:edit")
    public AjaxResult edit(
            @PathParam("teamId") Long teamId,
            @PathParam("fieldId") Long fieldId,
            SysProjectDefectField field) {
        field.setTeamId(teamId);
        field.setFieldId(fieldId);
        return toAjax(defectFieldService.update(field));
    }

    @DELETE
    @Path("/{fieldId}")
    @RequiresPermissions("system:team:defect-field:remove")
    public AjaxResult remove(@PathParam("teamId") Long teamId, @PathParam("fieldId") Long fieldId) {
        return toAjax(defectFieldService.softDelete(teamId, fieldId));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
