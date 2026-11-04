package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.ModulePageSettings;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysModulePageSettingsService;
import com.cat2bug.system.service.mcp.McpAccessService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/system/team/{teamId}/module-page-settings")
@Produces(MediaType.APPLICATION_JSON)
public class TeamModulePageSettingsResource {

    @Inject
    SysModulePageSettingsService sysModulePageSettingsService;

    @Inject
    McpAccessService mcpAccessService;

    @GET
    @RequiresPermissions(value = {"system:team:edit", "system:team:query"}, logical = RequiresPermissions.Logical.OR)
    public AjaxResult get(@PathParam("teamId") Long teamId) {
        return AjaxResult.success(sysModulePageSettingsService.viewByTeamId(teamId));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:edit")
    public AjaxResult save(@PathParam("teamId") Long teamId, ModulePageSettings body) {
        AjaxResult result = AjaxResult.success(sysModulePageSettingsService.saveByTeamId(teamId, body));
        mcpAccessService.invalidateAll();
        return result;
    }
}
