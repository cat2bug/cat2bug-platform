package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysTeamService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/team")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminTeamResource {

    @Inject
    SysTeamService sysTeamService;

    @GET
    @Path("/list")
    @RequiresPermissions("admin:team:list")
    public TableDataInfo list(@QueryParam("teamName") String teamName,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        return sysTeamService.selectTeamPage(teamName, pageNum, pageSize);
    }

    @PUT
    @Path("/{teamId}/lock")
    @RequiresPermissions("admin:team:lock")
    public AjaxResult lock(@PathParam("teamId") Long teamId, SysTeam team) {
        SysTeam updateTeam = new SysTeam();
        updateTeam.setTeamId(teamId);
        updateTeam.setLock(team.getLock());
        updateTeam.setLockRemark(team.getLockRemark());
        return toAjax(sysTeamService.updateSysTeam(updateTeam));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
