package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.UserExcelExportService;
import com.cat2bug.quarkus.admin.service.UserImportTemplateService;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

/**
 * 前端成员管理页使用 {@code /system/member/*} 路径（与 Spring 菜单路由一致），
 * Excel 导出/模版与 {@link UserResource} 共用同一套服务。
 */
@Path("/system/member")
public class MemberResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    UserExcelExportService userExcelExportService;

    @Inject
    UserImportTemplateService userImportTemplateService;

    @Inject
    UserResource userResource;

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions(value = {"system:user:export", "system:member:export"},
            logical = RequiresPermissions.Logical.OR)
    public Response export(@RestForm("userName") String userName,
                           @RestForm("status") String status,
                           @RestForm("phonenumber") String phonenumber,
                           @RestForm("deptId") Long deptId) {
        SysUser query = new SysUser();
        query.setUserName(userName);
        query.setStatus(status);
        query.setPhoneNumber(phonenumber);
        query.setDeptId(deptId);
        byte[] workbook = userExcelExportService.buildExportWorkbook(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @POST
    @Path("/importTemplate")
    @Consumes(MediaType.WILDCARD)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    public Response importTemplate() {
        byte[] workbook = userImportTemplateService.buildTemplateWorkbook();
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @POST
    @Path("/importData")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RequiresPermissions(value = {"system:user:import", "system:member:import"},
            logical = RequiresPermissions.Logical.OR)
    public com.cat2bug.quarkus.common.api.AjaxResult importData(
            @RestForm("file") org.jboss.resteasy.reactive.multipart.FileUpload file,
            @RestForm("updateSupport") Boolean updateSupport) throws java.io.IOException {
        return userResource.importData(file, updateSupport);
    }
}
