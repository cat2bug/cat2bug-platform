package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.SysCaseType;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysCaseTypeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/system/team/{teamId}/case-types")
@Produces(MediaType.APPLICATION_JSON)
public class TeamCaseTypeResource {

    @Inject
    SysCaseTypeService sysCaseTypeService;

    @GET
    @RequiresPermissions("system:team:case-type:list")
    public AjaxResult list(@PathParam("teamId") Long teamId,
                           @QueryParam("enabledOnly") @DefaultValue("false") boolean enabledOnly) {
        return AjaxResult.success(sysCaseTypeService.listByTeamId(teamId, enabledOnly));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:case-type:add")
    public AjaxResult add(@PathParam("teamId") Long teamId, SysCaseType type) {
        return AjaxResult.success(sysCaseTypeService.create(teamId, type == null ? null : type.getTypeName()));
    }

    @PUT
    @Path("/{typeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:case-type:edit")
    public AjaxResult update(@PathParam("teamId") Long teamId, @PathParam("typeId") Long typeId, SysCaseType type) {
        if (type != null && type.getEnabled() != null) {
            sysCaseTypeService.setEnabled(teamId, typeId, type.getEnabled());
        }
        if (type != null && type.getTypeName() != null && !type.getTypeName().isBlank()) {
            sysCaseTypeService.rename(teamId, typeId, type.getTypeName());
        }
        if (type == null || (type.getEnabled() == null && (type.getTypeName() == null || type.getTypeName().isBlank()))) {
            throw new ApiException("请指定类型名称或启用状态");
        }
        return AjaxResult.success();
    }

    @DELETE
    @Path("/{typeId}")
    @RequiresPermissions("system:team:case-type:remove")
    public AjaxResult remove(@PathParam("teamId") Long teamId, @PathParam("typeId") Long typeId) {
        sysCaseTypeService.delete(teamId, typeId);
        return AjaxResult.success();
    }
}
