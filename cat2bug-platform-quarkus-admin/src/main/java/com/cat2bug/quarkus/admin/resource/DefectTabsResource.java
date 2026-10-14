package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysProjectDefectTabs;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysProjectDefectTabsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;

@Path("/system/tabs")
@Produces(MediaType.APPLICATION_JSON)
public class DefectTabsResource {

    @Inject
    SysProjectDefectTabsService sysProjectDefectTabsService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:defect:list")
    public TableDataInfo list(@QueryParam("tabName") String tabName,
                              @QueryParam("projectId") Long projectId,
                              @QueryParam("userId") Long userId,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysProjectDefectTabs query = new SysProjectDefectTabs();
        query.setTabName(tabName);
        query.setProjectId(projectId);
        query.setUserId(userId);
        return sysProjectDefectTabsService.selectTabsPage(query, pageNum, pageSize);
    }

    @GET
    @Path("/{tabId}")
    @RequiresPermissions("system:defect:list")
    public AjaxResult getInfo(@PathParam("tabId") Long tabId) {
        return AjaxResult.success(sysProjectDefectTabsService.selectSysProjectDefectTabsByTabId(tabId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:list")
    public AjaxResult add(SysProjectDefectTabs tabs) {
        return AjaxResult.success(sysProjectDefectTabsService.insertSysProjectDefectTabs(tabs));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:list")
    public AjaxResult edit(SysProjectDefectTabs tabs) {
        return toAjax(sysProjectDefectTabsService.updateSysProjectDefectTabs(tabs));
    }

    @PUT
    @Path("/sort")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:list")
    public AjaxResult updateSort(List<SysProjectDefectTabs> list) {
        return toAjax(sysProjectDefectTabsService.updateSort(list));
    }

    @DELETE
    @Path("/{tabIds}")
    @RequiresPermissions("system:defect:list")
    public AjaxResult remove(@PathParam("tabIds") String tabIds) {
        Long[] ids = Arrays.stream(tabIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysProjectDefectTabsService.deleteSysProjectDefectTabsByTabIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
