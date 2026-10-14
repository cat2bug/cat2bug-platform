package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.SysModule;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysModuleService;
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
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

@Path("/system/module")
@Produces(MediaType.APPLICATION_JSON)
public class ModuleResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysModuleService sysModuleService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {"system:defect:list", "system:module:list"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult list(@QueryParam("projectId") Long projectId,
                           @QueryParam("modulePid") Long modulePid,
                           @QueryParam("moduleName") String moduleName) {
        SysModule query = new SysModule();
        query.setProjectId(projectId);
        query.setModulePid(modulePid);
        query.setModuleName(moduleName);
        return AjaxResult.success(sysModuleService.selectSysModuleList(query));
    }

    @GET
    @Path("/path-list")
    @RequiresPermissions(value = {"system:defect:list", "system:module:list"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult pathList(@QueryParam("projectId") Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        return AjaxResult.success(sysModuleService.selectSysModulePathList(projectId));
    }

    @GET
    @Path("/tree")
    @RequiresPermissions(value = {"system:defect:list", "system:module:list"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult tree(@QueryParam("projectId") Long projectId,
                           @QueryParam("modulePid") Long modulePid,
                           @QueryParam("moduleName") String moduleName) {
        SysModule query = new SysModule();
        query.setProjectId(projectId);
        query.setModulePid(modulePid);
        query.setModuleName(moduleName);
        return AjaxResult.success(sysModuleService.selectModuleTree(query));
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:module:export")
    public Response export(@RestForm("projectId") Long projectId,
                           @RestForm("modulePid") Long modulePid,
                           @RestForm("moduleName") String moduleName) {
        SysModule query = new SysModule();
        query.setProjectId(projectId);
        query.setModulePid(modulePid);
        query.setModuleName(moduleName);
        byte[] workbook = systemExcelExportService.exportModules(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{moduleId}")
    @RequiresPermissions("system:module:query")
    public AjaxResult getInfo(@PathParam("moduleId") Long moduleId) {
        return AjaxResult.success(sysModuleService.selectSysModuleByModuleId(moduleId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:module:add")
    public AjaxResult add(SysModule sysModule) {
        return AjaxResult.success(sysModuleService.insertSysModule(sysModule));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:module:edit")
    public AjaxResult edit(SysModule sysModule) {
        int rows = sysModuleService.updateSysModule(sysModule);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("更新失败");
    }

    @DELETE
    @Path("/{moduleIds}")
    @RequiresPermissions("system:module:remove")
    public AjaxResult remove(@PathParam("moduleIds") String moduleIds) {
        Long[] ids = parseIds(moduleIds);
        int rows = sysModuleService.deleteSysModuleByModuleIds(ids);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("删除失败");
    }

    private static Long[] parseIds(String moduleIds) {
        String[] parts = moduleIds.split(",");
        Long[] ids = new Long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ids[i] = Long.parseLong(parts[i].trim());
        }
        return ids;
    }
}
