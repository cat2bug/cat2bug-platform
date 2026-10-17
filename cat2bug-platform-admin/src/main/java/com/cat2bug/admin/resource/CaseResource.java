package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.CaseExcelExportService;
import com.cat2bug.admin.service.CaseExcelImportService;
import com.cat2bug.admin.service.CaseImportTemplateService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.exception.CaseImportValidationException;
import com.cat2bug.common.excel.ExcelColumnExportSupport;
import com.cat2bug.common.util.LocaleUtils;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.LoginUserResolver;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.MemberFocusService;
import com.cat2bug.system.service.SysCaseService;
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
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Path("/system/case")
@Produces(MediaType.APPLICATION_JSON)
public class CaseResource {

    private static final String MODULE_NAME = "case";
    private static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysCaseService sysCaseService;

    @Inject
    CaseExcelExportService caseExcelExportService;

    @Inject
    CaseExcelImportService caseExcelImportService;

    @Inject
    CaseImportTemplateService caseImportTemplateService;

    @Inject
    LoginUserResolver loginUserResolver;

    @Inject
    MemberFocusService memberFocusService;

    @GET
    @Path("/module/{moduleId}/ids")
    @RequiresPermissions(value = {"system:case:list", "system:plan:add", "system:plan:edit"},
            logical = RequiresPermissions.Logical.OR)
    public TableDataInfo moduleIds(@PathParam("moduleId") Long moduleId) {
        var ids = sysCaseService.selectSysCaseIdList(moduleId);
        return new TableDataInfo(ids, ids.size());
    }

    @GET
    @Path("/level/{level}/ids")
    @RequiresPermissions(value = {"system:case:list", "system:plan:add", "system:plan:edit"},
            logical = RequiresPermissions.Logical.OR)
    public TableDataInfo levelIds(@PathParam("level") Long level) {
        var ids = sysCaseService.selectSysCaseIdByLevelList(level);
        return new TableDataInfo(ids, ids.size());
    }

    @GET
    @Path("/list")
    @RequiresPermissions("system:case:list")
    public TableDataInfo list(@Context UriInfo uriInfo,
                              @QueryParam("projectId") Long projectId,
                              @QueryParam("caseName") String caseName,
                              @QueryParam("moduleId") Long moduleId,
                              @QueryParam("caseType") Long caseType,
                              @QueryParam("caseLevel") Long caseLevel,
                              @QueryParam("caseNum") Long caseNum,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysCase query = new SysCase();
        query.setProjectId(projectId);
        query.setCaseName(caseName);
        query.setModuleId(moduleId);
        query.setCaseType(caseType);
        query.setCaseLevel(caseLevel);
        query.setCaseNum(caseNum);
        sysCaseService.restrictToModuleTree(query, longQuery(uriInfo, "params[modulePid]"));
        TableDataInfo table = sysCaseService.selectCasePage(query, pageNum, pageSize);
        enrichCaseFocusList(table);
        return table;
    }

    @GET
    @Path("/{caseId}")
    @RequiresPermissions("system:case:query")
    public AjaxResult getInfo(@Context ContainerRequestContext requestContext,
                              @PathParam("caseId") Long caseId) {
        SysCase sysCase = sysCaseService.selectSysCaseByCaseId(caseId);
        if (sysCase != null) {
            LoginUser loginUser = loginUserResolver.resolve(requestContext);
            memberFocusService.setFocus(MODULE_NAME, caseId, loginUser.getUser());
            sysCase.setFocusList(memberFocusService.getFocusMemberList(MODULE_NAME, caseId));
        }
        return AjaxResult.success(sysCase);
    }

    private void enrichCaseFocusList(TableDataInfo table) {
        if (table.getRows() == null) {
            return;
        }
        for (Object row : table.getRows()) {
            if (row instanceof SysCase sysCase) {
                sysCase.setFocusList(memberFocusService.getFocusMemberList(MODULE_NAME, sysCase.getCaseId()));
            }
        }
    }

    @GET
    @Path("/{caseId}/prev")
    @RequiresPermissions("system:case:query")
    public AjaxResult prev(@PathParam("caseId") Long caseId,
                           @QueryParam("projectId") Long projectId,
                           @QueryParam("caseName") String caseName,
                           @QueryParam("moduleId") Long moduleId,
                           @QueryParam("caseType") Long caseType,
                           @QueryParam("caseLevel") Long caseLevel,
                           @QueryParam("caseNum") Long caseNum,
                           @QueryParam("params[search]") String search) {
        SysCase query = buildNavQuery(projectId, caseName, moduleId, caseType, caseLevel, caseNum, search);
        return AjaxResult.success(sysCaseService.selectPrevSysCase(query, caseId));
    }

    @GET
    @Path("/{caseId}/next")
    @RequiresPermissions("system:case:query")
    public AjaxResult next(@PathParam("caseId") Long caseId,
                           @QueryParam("projectId") Long projectId,
                           @QueryParam("caseName") String caseName,
                           @QueryParam("moduleId") Long moduleId,
                           @QueryParam("caseType") Long caseType,
                           @QueryParam("caseLevel") Long caseLevel,
                           @QueryParam("caseNum") Long caseNum,
                           @QueryParam("params[search]") String search) {
        SysCase query = buildNavQuery(projectId, caseName, moduleId, caseType, caseLevel, caseNum, search);
        return AjaxResult.success(sysCaseService.selectNextSysCase(query, caseId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:add")
    public AjaxResult add(SysCase sysCase) {
        return AjaxResult.success(sysCaseService.insertSysCase(sysCase));
    }

    @POST
    @Path("/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:add")
    public AjaxResult batchAdd(java.util.List<SysCase> list) {
        return AjaxResult.success(sysCaseService.batchInsertSysCase(list));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:edit")
    public AjaxResult edit(SysCase sysCase) {
        int rows = sysCaseService.updateSysCase(sysCase);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("更新失败");
    }

    @DELETE
    @Path("/{caseId}")
    @RequiresPermissions("system:case:remove")
    public AjaxResult remove(@PathParam("caseId") Long caseId) {
        int rows = sysCaseService.deleteSysCaseByCaseId(caseId);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("删除失败");
    }

    @POST
    @Path("/close-edit-window")
    @Consumes({MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    public AjaxResult closeEditWindow() {
        sysCaseService.closeEditWindow();
        return AjaxResult.success();
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:case:export")
    public Response export(@Context HttpHeaders headers,
                           @RestForm("projectId") Long projectId,
                           @RestForm("caseName") String caseName,
                           @RestForm("moduleId") Long moduleId,
                           @RestForm("caseType") Long caseType,
                           @RestForm("caseLevel") Long caseLevel,
                           @RestForm("caseNum") Long caseNum,
                           @RestForm("params[exportScope]") String exportScope,
                           @RestForm("params[exportColumns]") String exportColumns,
                           @RestForm("params[host]") String host,
                           @RestForm("params[modulePid]") Long modulePid) {
        SysCase query = new SysCase();
        query.setProjectId(projectId);
        query.setCaseName(caseName);
        query.setModuleId(moduleId);
        query.setCaseType(caseType);
        query.setCaseLevel(caseLevel);
        query.setCaseNum(caseNum);
        sysCaseService.restrictToModuleTree(query, modulePid);
        Map<String, Object> params = buildExportParams(exportScope, exportColumns, host);
        byte[] workbook = caseExcelExportService.buildExportWorkbook(query, params, resolveLocale(headers));
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @POST
    @Path("/importData")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequiresPermissions("system:case:import")
    public AjaxResult importData(@Context HttpHeaders headers,
                                 @RestForm("file") FileUpload file,
                                 @RestForm("projectId") Long projectId) throws IOException {
        if (file == null || file.uploadedFile() == null) {
            return AjaxResult.error("上传文件不能为空");
        }
        try (var input = Files.newInputStream(file.uploadedFile())) {
            var caseList = caseExcelImportService.parseWorkbook(input, projectId, Map.of(), resolveLocale(headers));
            try {
                return AjaxResult.success(sysCaseService.importCase(caseList, projectId));
            } catch (CaseImportValidationException ex) {
                return AjaxResult.success(ex.getResult());
            }
        }
    }

    @POST
    @Path("/importTemplate")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:case:add")
    public Response importTemplate(@Context HttpHeaders headers,
                                   @QueryParam("projectId") Long projectId,
                                   @RestForm("params[exportScope]") String exportScope,
                                   @RestForm("params[exportColumns]") String exportColumns) {
        Map<String, Object> params = buildExportParams(exportScope, exportColumns, null);
        byte[] workbook = caseImportTemplateService.buildTemplateWorkbook(projectId, params, resolveLocale(headers));
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    private static Long longQuery(UriInfo uriInfo, String name) {
        if (uriInfo == null) {
            return null;
        }
        String raw = uriInfo.getQueryParameters().getFirst(name);
        if (StringUtils.isEmpty(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static SysCase buildNavQuery(Long projectId, String caseName, Long moduleId, Long caseType,
                                         Long caseLevel, Long caseNum, String search) {
        SysCase query = new SysCase();
        query.setProjectId(projectId);
        query.setCaseName(caseName);
        query.setModuleId(moduleId);
        query.setCaseType(caseType);
        query.setCaseLevel(caseLevel);
        query.setCaseNum(caseNum);
        if (StringUtils.isNotEmpty(search)) {
            Map<String, Object> params = new HashMap<>();
            params.put("search", search);
            query.setParams(params);
        }
        return query;
    }

    private Map<String, Object> buildExportParams(String exportScope, String exportColumns, String host) {
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotEmpty(exportScope)) {
            params.put(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, exportScope);
        }
        if (StringUtils.isNotEmpty(exportColumns)) {
            params.put(ExcelColumnExportSupport.PARAM_EXPORT_COLUMNS, exportColumns);
        }
        if (StringUtils.isNotEmpty(host)) {
            params.put("host", host.trim());
        }
        return params;
    }

    private Locale resolveLocale(HttpHeaders headers) {
        if (headers == null) {
            return LocaleUtils.parseLanguageHeader(null);
        }
        return LocaleUtils.parseLanguageHeader(headers.getHeaderString("language"));
    }
}
