package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.DefectPageSettings;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDefectPageSettingsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/system/team/{teamId}/defect-page-settings")
@Produces(MediaType.APPLICATION_JSON)
public class TeamDefectPageSettingsResource {

    @Inject
    SysDefectPageSettingsService sysDefectPageSettingsService;

    @GET
    @RequiresPermissions("system:team:defect-field:list")
    public AjaxResult get(@PathParam("teamId") Long teamId) {
        return AjaxResult.success(sysDefectPageSettingsService.getByTeamId(teamId));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:team:defect-field:edit")
    public AjaxResult save(@PathParam("teamId") Long teamId, DefectPageSettings body) {
        return AjaxResult.success(sysDefectPageSettingsService.saveByTeamId(teamId, body));
    }
}
