package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysUserStatisticTemplate;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysUserStatisticTemplateService;
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

import java.util.Arrays;

@Path("/system/statistic")
@Produces(MediaType.APPLICATION_JSON)
public class StatisticTemplateResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysUserStatisticTemplateService sysUserStatisticTemplateService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:project:list")
    public TableDataInfo list(@QueryParam("statisticTemplatCode") String statisticTemplatCode,
                              @QueryParam("moduleType") Long moduleType,
                              @QueryParam("projectId") Long projectId,
                              @QueryParam("userId") Long userId,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysUserStatisticTemplate query = new SysUserStatisticTemplate();
        query.setStatisticTemplatCode(statisticTemplatCode);
        query.setModuleType(moduleType);
        query.setProjectId(projectId);
        query.setUserId(userId);
        return sysUserStatisticTemplateService.selectTemplatePage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:statistic:export")
    public Response export(@RestForm("statisticTemplatCode") String statisticTemplatCode,
                           @RestForm("moduleType") Long moduleType,
                           @RestForm("projectId") Long projectId,
                           @RestForm("userId") Long userId) {
        SysUserStatisticTemplate query = new SysUserStatisticTemplate();
        query.setStatisticTemplatCode(statisticTemplatCode);
        query.setModuleType(moduleType);
        query.setProjectId(projectId);
        query.setUserId(userId);
        byte[] workbook = systemExcelExportService.exportStatisticTemplates(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{statisticTemplateId}")
    @RequiresPermissions("system:project:query")
    public AjaxResult getInfo(@PathParam("statisticTemplateId") Long statisticTemplateId) {
        return AjaxResult.success(sysUserStatisticTemplateService.selectById(statisticTemplateId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:list")
    public AjaxResult add(SysUserStatisticTemplate template) {
        return toAjax(sysUserStatisticTemplateService.insertSysUserStatisticTemplate(template));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:project:list")
    public AjaxResult edit(SysUserStatisticTemplate template) {
        return toAjax(sysUserStatisticTemplateService.updateSysUserStatisticTemplate(template));
    }

    @DELETE
    @Path("/{statisticTemplateIds}")
    @RequiresPermissions("system:project:list")
    public AjaxResult remove(@PathParam("statisticTemplateIds") String statisticTemplateIds) {
        Long[] ids = Arrays.stream(statisticTemplateIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toArray(Long[]::new);
        return toAjax(sysUserStatisticTemplateService.deleteByIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
