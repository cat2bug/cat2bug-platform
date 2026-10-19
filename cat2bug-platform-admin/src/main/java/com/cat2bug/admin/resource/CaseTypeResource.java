package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysCaseType;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysCaseTypeService;
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

@Path("/system/project/{projectId}/case-types")
@Produces(MediaType.APPLICATION_JSON)
public class CaseTypeResource {

    @Inject
    SysCaseTypeService sysCaseTypeService;

    @GET
    @RequiresPermissions(value = {"system:case:list", "system:project:case-type:list", "system:plan:add", "system:plan:edit"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult list(@PathParam("projectId") Long projectId) {
        return AjaxResult.success(sysCaseTypeService.list(projectId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:case-type:add")
    public AjaxResult add(@PathParam("projectId") Long projectId, SysCaseType type) {
        return AjaxResult.success(sysCaseTypeService.create(projectId, type == null ? null : type.getTypeName()));
    }

    @PUT
    @Path("/{typeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:case-type:edit")
    public AjaxResult rename(@PathParam("projectId") Long projectId, @PathParam("typeId") Long typeId, SysCaseType type) {
        sysCaseTypeService.rename(projectId, typeId, type == null ? null : type.getTypeName());
        return AjaxResult.success();
    }

    @DELETE
    @Path("/{typeId}")
    @RequiresPermissions("system:project:case-type:remove")
    public AjaxResult remove(@PathParam("projectId") Long projectId, @PathParam("typeId") Long typeId) {
        sysCaseTypeService.delete(projectId, typeId);
        return AjaxResult.success();
    }
}
