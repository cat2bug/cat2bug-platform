package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDocument;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.PermissionChecker;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysDocumentService;
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

@Path("/system/document")
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysDocumentService sysDocumentService;

    @Inject
    AuthContext authContext;

    @Inject
    PermissionChecker permissionChecker;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:document:list")
    public TableDataInfo list(@QueryParam("projectId") Long projectId,
                              @QueryParam("docId") Long docId,
                              @QueryParam("docName") String docName,
                              @QueryParam("docType") Long docType,
                              @QueryParam("fileExtension") String fileExtension,
                              @QueryParam("createById") Long createById,
                              @QueryParam("updateById") Long updateById,
                              @QueryParam("docPid") Long docPid,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysDocument query = new SysDocument();
        query.setProjectId(projectId);
        query.setDocId(docId);
        query.setDocName(docName);
        query.setDocType(docType);
        query.setFileExtension(fileExtension);
        query.setCreateById(createById);
        query.setUpdateById(updateById);
        query.setDocPid(docPid);
        return sysDocumentService.selectDocumentPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:document:export")
    public Response export(@RestForm("projectId") Long projectId,
                           @RestForm("docName") String docName,
                           @RestForm("docType") Long docType,
                           @RestForm("fileExtension") String fileExtension,
                           @RestForm("docPid") Long docPid) {
        SysDocument query = new SysDocument();
        query.setProjectId(projectId);
        query.setDocName(docName);
        query.setDocType(docType);
        query.setFileExtension(fileExtension);
        query.setDocPid(docPid);
        byte[] workbook = systemExcelExportService.exportDocuments(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{docId}")
    @RequiresPermissions("system:document:query")
    public AjaxResult getInfo(@PathParam("docId") Long docId) {
        return AjaxResult.success(sysDocumentService.selectSysDocumentByDocId(docId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:document:add")
    public AjaxResult add(SysDocument sysDocument) {
        int rows = sysDocumentService.insertSysDocument(sysDocument);
        if (rows <= 0) {
            return AjaxResult.error("操作失败");
        }
        return AjaxResult.success(sysDocument.getDocId());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:document:edit")
    public AjaxResult edit(SysDocument sysDocument) {
        return toAjax(sysDocumentService.updateSysDocument(sysDocument));
    }

    @DELETE
    @Path("/{docIds}")
    public AjaxResult remove(@PathParam("docIds") String docIds) {
        LoginUser loginUser = requireLoginUser();
        Long[] ids = Arrays.stream(docIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toArray(Long[]::new);
        if (!permissionChecker.hasPermi(loginUser, "system:document:remove")) {
            for (Long id : ids) {
                SysDocument doc = sysDocumentService.selectSysDocumentByDocId(id);
                if (doc == null || !loginUser.getUserId().equals(doc.getCreateById())) {
                    throw new ApiException(403, "没有权限");
                }
            }
        }
        return toAjax(sysDocumentService.deleteSysDocumentByDocIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
