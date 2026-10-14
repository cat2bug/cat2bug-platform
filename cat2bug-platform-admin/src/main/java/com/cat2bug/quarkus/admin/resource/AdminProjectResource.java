package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysProject;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysProjectService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/admin/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminProjectResource {

    @Inject
    SysProjectService sysProjectService;

    @GET
    @Path("/list")
    @RequiresPermissions("admin:project:list")
    public TableDataInfo list(@QueryParam("teamId") Long teamId,
                              @QueryParam("teamName") String teamName,
                              @QueryParam("projectName") String projectName,
                              @QueryParam("createBy") String createBy,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysProject query = new SysProject();
        query.setTeamId(teamId);
        query.setTeamName(teamName);
        query.setProjectName(projectName);
        query.setCreateBy(createBy);
        return sysProjectService.selectAdminProjectPage(query, pageNum, pageSize);
    }

    @PUT
    @Path("/{projectId}/lock")
    @RequiresPermissions("admin:project:lock")
    public AjaxResult lock(@PathParam("projectId") Long projectId, SysProject project) {
        SysProject updateProject = new SysProject();
        updateProject.setProjectId(projectId);
        updateProject.setLock(project.getLock());
        updateProject.setLockRemark(project.getLockRemark());
        return toAjax(sysProjectService.updateSysProject(updateProject));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
