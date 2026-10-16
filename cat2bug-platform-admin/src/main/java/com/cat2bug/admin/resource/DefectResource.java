package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.DefectExcelService;
import com.cat2bug.admin.service.DefectImNoticeService;
import com.cat2bug.admin.service.DefectImportTemplateService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.ImportDefectResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.common.domain.SysDefectShard;
import com.cat2bug.common.domain.SysUserDefect;
import com.cat2bug.common.excel.ExcelColumnExportSupport;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.LocaleUtils;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.LoginUserResolver;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.MemberFocusService;
import com.cat2bug.system.service.SysDefectService;
import com.cat2bug.system.service.SysDefectShardService;
import com.cat2bug.system.service.SysUserDefectService;
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
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import jakarta.ws.rs.DefaultValue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.ws.rs.core.Response;

@Path("/system/defect")
@Produces(MediaType.APPLICATION_JSON)
public class DefectResource {

    private static final String MODULE_NAME = "defect";
    private static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysDefectService sysDefectService;

    @Inject
    SysDefectShardService sysDefectShardService;

    @Inject
    LoginUserResolver loginUserResolver;

    @Inject
    DefectImportTemplateService defectImportTemplateService;

    @Inject
    DefectExcelService defectExcelService;

    @Inject
    SysUserDefectService sysUserDefectService;

    @Inject
    MemberFocusService memberFocusService;

    @Inject
    DefectImNoticeService defectImNoticeService;

    @GET
    @Path("/config")
    @RequiresPermissions("system:defect:list")
    public AjaxResult config() {
        return AjaxResult.success(sysDefectService.getDefectConfig());
    }

    @GET
    @Path("/state")
    @RequiresPermissions("system:defect:query")
    public AjaxResult states() {
        return AjaxResult.success(sysDefectService.getDefectStateList());
    }

    @GET
    @Path("/list")
    @RequiresPermissions("system:defect:list")
    public TableDataInfo list(@Context UriInfo uriInfo) {
        TableDataInfo table = sysDefectService.selectDefectPage(
                DefectListQuery.bind(uriInfo.getQueryParameters()),
                DefectListQuery.pageNum(uriInfo.getQueryParameters()),
                DefectListQuery.pageSize(uriInfo.getQueryParameters()));
        enrichDefectFocusList(table);
        return table;
    }

    @GET
    @Path("/{defectId}")
    @RequiresPermissions("system:defect:query")
    public AjaxResult getInfo(@Context ContainerRequestContext requestContext,
                              @PathParam("defectId") Long defectId) {
        SysDefect sysDefect = sysDefectService.selectSysDefectByDefectId(defectId);
        if (sysDefect != null) {
            LoginUser loginUser = loginUserResolver.resolve(requestContext);
            memberFocusService.setFocus(MODULE_NAME, defectId, loginUser.getUser());
            sysDefect.setFocusList(memberFocusService.getFocusMemberList(MODULE_NAME, defectId));
        }
        return AjaxResult.success(sysDefect);
    }

    private void enrichDefectFocusList(TableDataInfo table) {
        if (table.getRows() == null) {
            return;
        }
        for (Object row : table.getRows()) {
            if (row instanceof SysDefect defect) {
                defect.setFocusList(memberFocusService.getFocusMemberList(MODULE_NAME, defect.getDefectId()));
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:add")
    public AjaxResult add(SysDefect sysDefect) {
        SysDefect created = sysDefectService.insertSysDefect(sysDefect);
        dispatchDefectNotice(null, created.getDefectId());
        return AjaxResult.success(created);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:edit")
    public AjaxResult edit(SysDefect sysDefect) {
        int rows = sysDefectService.updateSysDefect(sysDefect);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("更新失败");
    }

    @PUT
    @Path("/{defectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"system:defect:list", "system:defect:query"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult editUserDefect(@PathParam("defectId") Long defectId, SysUserDefect sysUserDefect) {
        sysUserDefect.setDefectId(defectId);
        int rows = sysUserDefectService.updateSysUserDefect(sysUserDefect);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("更新失败");
    }

    @DELETE
    @Path("/{defectId}")
    @RequiresPermissions(value = {"system:defect:remove", "system:defect:edit", "system:defect:add"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult remove(@PathParam("defectId") Long defectId) {
        int rows = sysDefectService.deleteSysDefectByDefectId(defectId);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("删除失败");
    }

    @PUT
    @Path("/{defectId}/restore")
    @RequiresPermissions(value = {"system:defect:remove", "system:defect:edit", "system:defect:add"},
            logical = RequiresPermissions.Logical.OR)
    public AjaxResult restore(@PathParam("defectId") Long defectId) {
        int rows = sysDefectService.restoreSysDefectByDefectId(defectId);
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("恢复失败");
    }

    @POST
    @Path("/{defectId}/close-edit-window")
    @Consumes({MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    public AjaxResult closeEditWindow(@PathParam("defectId") Long defectId) {
        sysDefectService.closeEditWindow(defectId);
        return AjaxResult.success();
    }

    @GET
    @Path("/{defectId}/shard/{defectShardId}")
    public AjaxResult getSharedDefectInfo(@Context ContainerRequestContext requestContext,
                                          @PathParam("defectId") Long defectId,
                                          @PathParam("defectShardId") String defectShardId,
                                          @QueryParam("password") String password) {
        Long memberId = resolveMemberId(requestContext);
        Map<String, Object> result = sysDefectShardService.buildSharedDefectView(defectShardId, password, memberId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return AjaxResult.success(result.get("data"));
        }
        return AjaxResult.error(String.valueOf(result.get("message")));
    }

    @GET
    @Path("/{defectId}/shard")
    @RequiresPermissions("system:defect:list")
    public AjaxResult getShardInfo(@PathParam("defectId") Long defectId,
                                   @Context ContainerRequestContext requestContext) {
        LoginUser loginUser = loginUserResolver.resolve(requestContext);
        return AjaxResult.success(sysDefectShardService.selectSysDefectShardByDefectIdAndMemberId(
                defectId, loginUser.getUserId()));
    }

    @POST
    @Path("/{defectId}/shard")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:list")
    public AjaxResult shard(@PathParam("defectId") Long defectId, SysDefectShard sysDefectShard) {
        if (sysDefectShard.getDefectId() == null) {
            sysDefectShard.setDefectId(defectId);
        }
        return AjaxResult.success(sysDefectShardService.insertSysDefectShard(sysDefectShard));
    }

    @POST
    @Path("/{defectId}/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:assign")
    public AjaxResult assign(@PathParam("defectId") Long defectId, SysDefectLog log) {
        log.setDefectId(defectId);
        SysDefectLog result = sysDefectService.assign(log);
        dispatchDefectNotice(log.getSrcHost(), defectId);
        return AjaxResult.success(result);
    }

    @POST
    @Path("/{defectId}/reject")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:reject")
    public AjaxResult reject(@PathParam("defectId") Long defectId, SysDefectLog log) {
        log.setDefectId(defectId);
        SysDefectLog result = sysDefectService.reject(log);
        dispatchDefectNotice(log.getSrcHost(), defectId);
        return AjaxResult.success(result);
    }

    @POST
    @Path("/{defectId}/repair")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:repair")
    public AjaxResult repair(@PathParam("defectId") Long defectId, SysDefectLog log) {
        log.setDefectId(defectId);
        SysDefectLog result = sysDefectService.repair(log);
        dispatchDefectNotice(log.getSrcHost(), defectId);
        return AjaxResult.success(result);
    }

    @POST
    @Path("/{defectId}/pass")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:pass")
    public AjaxResult pass(@PathParam("defectId") Long defectId, SysDefectLog log) {
        log.setDefectId(defectId);
        SysDefectLog result = sysDefectService.pass(log);
        dispatchDefectNotice(log.getSrcHost(), defectId);
        return AjaxResult.success(result);
    }

    @POST
    @Path("/{defectId}/close")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:close")
    public AjaxResult close(@PathParam("defectId") Long defectId, SysDefectLog log) {
        log.setDefectId(defectId);
        SysDefectLog result = sysDefectService.close(log);
        dispatchDefectNotice(log.getSrcHost(), defectId);
        return AjaxResult.success(result);
    }

    @POST
    @Path("/{defectId}/open")
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:defect:open")
    public AjaxResult open(@PathParam("defectId") Long defectId, SysDefectLog log) {
        log.setDefectId(defectId);
        SysDefectLog result = sysDefectService.open(log);
        dispatchDefectNotice(log.getSrcHost(), defectId);
        return AjaxResult.success(result);
    }

    @POST
    @Path("/importTemplate")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:defect:add")
    public Response importTemplate(@Context HttpHeaders headers,
                                   @RestForm("params[exportScope]") String exportScope,
                                   @RestForm("params[exportColumns]") String exportColumns) {
        Map<String, Object> params = buildExportParams(exportScope, exportColumns);
        byte[] workbook = defectImportTemplateService.buildTemplateWorkbook(params, resolveLocale(headers));
        return Response.ok(workbook)
                .type(SPREADSHEET_MEDIA_TYPE)
                .build();
    }

    @POST
    @Path("/export")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON})
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:defect:add")
    public Response export(@Context HttpHeaders headers,
                           @RestForm("projectId") Long projectId,
                           @RestForm("defectName") String defectName,
                           @RestForm("defectState") String defectState,
                           @RestForm("defectType") String defectType,
                           @RestForm("moduleId") Long moduleId,
                           @RestForm("params[exportScope]") String exportScope,
                           @RestForm("params[exportColumns]") String exportColumns) {
        SysDefect query = new SysDefect();
        query.setProjectId(projectId);
        query.setDefectName(defectName);
        query.setDefectState(enumParam(DefectConstants::parseState, defectState));
        query.setDefectType(enumParam(DefectConstants::parseType, defectType));
        query.setModuleId(moduleId);
        Map<String, Object> params = buildExportParams(exportScope, exportColumns);
        byte[] workbook = defectExcelService.buildExportWorkbook(query, params, resolveLocale(headers));
        return Response.ok(workbook)
                .type(SPREADSHEET_MEDIA_TYPE)
                .build();
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequiresPermissions("system:defect:add")
    public AjaxResult importData(@Context HttpHeaders headers,
                                 @RestForm("file") FileUpload file,
                                 @RestForm("params[exportScope]") String exportScope,
                                 @RestForm("params[exportColumns]") String exportColumns,
                                 @QueryParam("updateSupport") @DefaultValue("false") boolean updateSupport)
            throws IOException {
        if (file == null || file.uploadedFile() == null) {
            return AjaxResult.error("上传文件不能为空");
        }
        byte[] fileBytes = Files.readAllBytes(file.uploadedFile());
        Long projectId = defectExcelService.resolveCurrentProjectId();
        Map<String, Object> params = buildExportParams(exportScope, exportColumns);
        var defectList = defectExcelService.parseImportWorkbook(fileBytes, projectId, params, resolveLocale(headers));
        ImportDefectResult result = sysDefectService.importDefect(projectId, defectList, updateSupport);
        if (!result.isSuccess()) {
            return AjaxResult.success(result.getErrorMessage(), null);
        }
        for (Long defectId : result.getInsertedDefectIds()) {
            dispatchDefectNotice(null, defectId);
        }
        return AjaxResult.success(String.format("导入成功,共导入%d条数据", defectList.size()), null);
    }

    static Integer enumParam(java.util.function.Function<String, Integer> parser, String value) {
        try {
            return parser.apply(value);
        } catch (IllegalArgumentException e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    private Map<String, Object> buildExportParams(String exportScope, String exportColumns) {
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotEmpty(exportScope)) {
            params.put(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, exportScope);
        }
        if (StringUtils.isNotEmpty(exportColumns)) {
            params.put(ExcelColumnExportSupport.PARAM_EXPORT_COLUMNS, exportColumns);
        }
        return params;
    }

    private Locale resolveLocale(HttpHeaders headers) {
        if (headers == null) {
            return LocaleUtils.parseLanguageHeader(null);
        }
        String language = headers.getHeaderString("language");
        return LocaleUtils.parseLanguageHeader(language);
    }

    private Long resolveMemberId(ContainerRequestContext requestContext) {
        LoginUser loginUser = loginUserResolver.resolve(requestContext);
        return loginUser != null ? loginUser.getUserId() : null;
    }

    private void dispatchDefectNotice(String srcHost, Long defectId) {
        defectImNoticeService.sendDefectNotice(srcHost, defectId);
    }
}
