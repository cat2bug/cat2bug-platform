package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.domain.report.ReportTemplatePickerResponse;
import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.admin.service.report.ReportPresetTemplateService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysReportTemplate;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysReportTemplateService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/system/report/template")
@Produces(MediaType.APPLICATION_JSON)
public class ReportTemplateResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysReportTemplateService sysReportTemplateService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @Inject
    ReportPresetTemplateService reportPresetTemplateService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:report:list")
    public TableDataInfo list(@QueryParam("moduleType") String moduleType,
                              @QueryParam("templateTitle") String templateTitle,
                              @QueryParam("updateById") Long updateById,
                              @QueryParam("projectId") Long projectId,
                              @QueryParam("templateIconUrl") String templateIconUrl,
                              @QueryParam("majorVersion") Long majorVersion,
                              @QueryParam("minorVersion") Long minorVersion,
                              @QueryParam("templateKey") String templateKey,
                              @QueryParam("isShop") Boolean isShop,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysReportTemplate query = new SysReportTemplate();
        query.setModuleType(moduleType);
        query.setTemplateTitle(templateTitle);
        query.setUpdateById(updateById);
        query.setProjectId(projectId);
        query.setTemplateIconUrl(templateIconUrl);
        query.setMajorVersion(majorVersion);
        query.setMinorVersion(minorVersion);
        query.setTemplateKey(templateKey);
        query.setIsShop(isShop);
        return sysReportTemplateService.selectTemplatePage(query, pageNum, pageSize);
    }

    /**
     * 分组选择器：自定义（DB）+ classpath 预制
     */
    @GET
    @Path("/picker")
    @RequiresPermissions("system:report:list")
    public AjaxResult picker(@QueryParam("projectId") Long projectId,
                             @QueryParam("pageNum") Integer pageNum,
                             @QueryParam("pageSize") Integer pageSize,
                             @QueryParam("templateTitle") String templateTitle) {
        SysReportTemplate query = new SysReportTemplate();
        query.setProjectId(projectId);
        String titleFilter = templateTitle;
        if (titleFilter != null && !titleFilter.isBlank()) {
            query.setTemplateTitle(titleFilter.trim());
        }
        List<SysReportTemplate> customs;
        Long customTotal = null;
        if (pageNum != null && pageSize != null) {
            TableDataInfo page = sysReportTemplateService.selectTemplatePage(query, pageNum, pageSize);
            @SuppressWarnings("unchecked")
            List<SysReportTemplate> rows = (List<SysReportTemplate>) page.getRows();
            customs = rows;
            customTotal = page.getTotal();
        } else {
            customs = sysReportTemplateService.selectTemplateListAll(query);
        }
        ReportTemplatePickerResponse response =
                reportPresetTemplateService.buildPicker(customs, templateTitle);
        if (customTotal != null
                && response.getGroups() != null
                && !response.getGroups().isEmpty()) {
            response.getGroups().get(0).setTotal(customTotal);
        }
        return AjaxResult.success(response);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:report:export")
    public Response export(@RestForm("moduleType") String moduleType,
                           @RestForm("templateTitle") String templateTitle,
                           @RestForm("updateById") Long updateById,
                           @RestForm("projectId") Long projectId,
                           @RestForm("templateIconUrl") String templateIconUrl,
                           @RestForm("majorVersion") Long majorVersion,
                           @RestForm("minorVersion") Long minorVersion,
                           @RestForm("templateKey") String templateKey,
                           @RestForm("isShop") Boolean isShop) {
        SysReportTemplate query = new SysReportTemplate();
        query.setModuleType(moduleType);
        query.setTemplateTitle(templateTitle);
        query.setUpdateById(updateById);
        query.setProjectId(projectId);
        query.setTemplateIconUrl(templateIconUrl);
        query.setMajorVersion(majorVersion);
        query.setMinorVersion(minorVersion);
        query.setTemplateKey(templateKey);
        query.setIsShop(isShop);
        byte[] workbook = systemExcelExportService.exportReportTemplates(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/preset/{group}/{key}/content")
    @RequiresPermissions("system:report:list")
    public AjaxResult presetContent(@PathParam("group") String group,
                                    @PathParam("key") String key) {
        Map<String, String> body = new HashMap<>(2);
        body.put("content", reportPresetTemplateService.readMarkdownContent(group, key, null));
        return AjaxResult.success(body);
    }

    @GET
    @Path("/preset/{group}/{folder}/{key}/content")
    @RequiresPermissions("system:report:list")
    public AjaxResult presetContentWithFolder(@PathParam("group") String group,
                                              @PathParam("folder") String folder,
                                              @PathParam("key") String key) {
        Map<String, String> body = new HashMap<>(2);
        body.put("content", reportPresetTemplateService.readMarkdownContent(group, folder, key));
        return AjaxResult.success(body);
    }

    @GET
    @Path("/preset/{group}/{key}/preview")
    @Produces("image/png")
    public Response presetPreview(@PathParam("group") String group,
                                  @PathParam("key") String key) {
        byte[] png = reportPresetTemplateService.readPreviewPng(group, key, null);
        return Response.ok(png)
                .header("Cache-Control", "no-cache, max-age=0, must-revalidate")
                .type("image/png")
                .build();
    }

    @GET
    @Path("/preset/{group}/{folder}/{key}/preview")
    @Produces("image/png")
    public Response presetPreviewWithFolder(@PathParam("group") String group,
                                            @PathParam("folder") String folder,
                                            @PathParam("key") String key) {
        byte[] png = reportPresetTemplateService.readPreviewPng(group, folder, key);
        return Response.ok(png)
                .header("Cache-Control", "no-cache, max-age=0, must-revalidate")
                .type("image/png")
                .build();
    }

    @POST
    @Path("/preset/{group}/{key}/copy")
    @RequiresPermissions("system:report:add")
    public AjaxResult copyPreset(@PathParam("group") String group,
                                 @PathParam("key") String key,
                                 @QueryParam("projectId") Long projectId) {
        SysReportTemplate copy = reportPresetTemplateService.buildCustomCopy(projectId, group, key, null);
        return AjaxResult.success(sysReportTemplateService.insertSysReportTemplate(copy));
    }

    @POST
    @Path("/preset/{group}/{folder}/{key}/copy")
    @RequiresPermissions("system:report:add")
    public AjaxResult copyPresetWithFolder(@PathParam("group") String group,
                                           @PathParam("folder") String folder,
                                           @PathParam("key") String key,
                                           @QueryParam("projectId") Long projectId) {
        SysReportTemplate copy = reportPresetTemplateService.buildCustomCopy(projectId, group, folder, key);
        return AjaxResult.success(sysReportTemplateService.insertSysReportTemplate(copy));
    }

    @GET
    @Path("/{templateId}")
    @RequiresPermissions("system:report:list")
    public AjaxResult getInfo(@PathParam("templateId") Long templateId) {
        return AjaxResult.success(sysReportTemplateService.selectSysReportTemplateByTemplateId(templateId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:report:add")
    public AjaxResult add(SysReportTemplate sysReportTemplate) {
        return AjaxResult.success(sysReportTemplateService.insertSysReportTemplate(sysReportTemplate));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:report:add")
    public AjaxResult edit(SysReportTemplate sysReportTemplate) {
        return AjaxResult.success(sysReportTemplateService.updateSysReportTemplate(sysReportTemplate));
    }

    @DELETE
    @Path("/{templateIds}")
    @RequiresPermissions("system:report:add")
    public AjaxResult remove(@PathParam("templateIds") String templateIds) {
        Long[] ids = Arrays.stream(templateIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toArray(Long[]::new);
        return toAjax(sysReportTemplateService.deleteSysReportTemplateByTemplateIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
