package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysTeamPlanService;
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
import java.util.Map;

@Path("/admin/team")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminTeamResource {

    @Inject
    SysTeamService sysTeamService;

    @Inject
    SysTeamPlanService sysTeamPlanService;

    @GET
    @Path("/list")
    @RequiresPermissions("admin:team:list")
    public TableDataInfo list(@QueryParam("teamName") String teamName,
                              @QueryParam("planId") Long planId,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        return sysTeamService.selectTeamPage(teamName, planId, pageNum, pageSize);
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

    @PUT
    @Path("/{teamId}/plan")
    @RequiresPermissions("admin:team:plan")
    public AjaxResult bindPlan(@PathParam("teamId") Long teamId, Map<String, Object> body) {
        Object raw = body == null ? null : body.get("planId");
        if (raw == null) {
            return AjaxResult.error(MessageUtils.message("team.plan.bind-id-required"));
        }
        Long planId = raw instanceof Number ? ((Number) raw).longValue() : Long.parseLong(String.valueOf(raw));
        return toAjax(sysTeamPlanService.bindTeamPlan(teamId, planId));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error(MessageUtils.message("team.plan.operate-failed"));
    }
}
