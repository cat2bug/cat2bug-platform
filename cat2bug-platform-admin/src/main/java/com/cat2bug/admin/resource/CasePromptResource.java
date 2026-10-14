package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.SystemExcelExportService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.SysCasePrompt;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.system.service.SysCasePromptService;
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

@Path("/system/CasePrompt")
@Produces(MediaType.APPLICATION_JSON)
public class CasePromptResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SysCasePromptService sysCasePromptService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions("system:case:add")
    public TableDataInfo list(@QueryParam("casePromptContent") String casePromptContent,
                              @QueryParam("projectId") Long projectId,
                              @QueryParam("createById") Long createById,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        SysCasePrompt query = new SysCasePrompt();
        query.setCasePromptContent(casePromptContent);
        query.setProjectId(projectId);
        query.setCreateById(createById);
        return sysCasePromptService.selectCasePromptPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("system:case:add")
    public Response export(@RestForm("casePromptContent") String casePromptContent,
                           @RestForm("projectId") Long projectId,
                           @RestForm("createById") Long createById) {
        SysCasePrompt query = new SysCasePrompt();
        query.setCasePromptContent(casePromptContent);
        query.setProjectId(projectId);
        query.setCreateById(createById);
        byte[] workbook = systemExcelExportService.exportCasePrompts(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{casePromptId}")
    @RequiresPermissions("system:case:add")
    public AjaxResult getInfo(@PathParam("casePromptId") String casePromptId) {
        return AjaxResult.success(sysCasePromptService.selectSysCasePromptByCasePromptId(casePromptId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:add")
    public AjaxResult add(SysCasePrompt sysCasePrompt) {
        return toAjax(sysCasePromptService.insertSysCasePrompt(sysCasePrompt));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("system:case:add")
    public AjaxResult edit(SysCasePrompt sysCasePrompt) {
        return toAjax(sysCasePromptService.updateSysCasePrompt(sysCasePrompt));
    }

    @DELETE
    @Path("/{casePromptIds}")
    @RequiresPermissions("system:case:add")
    public AjaxResult remove(@PathParam("casePromptIds") String casePromptIds) {
        String[] ids = Arrays.stream(casePromptIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        return toAjax(sysCasePromptService.deleteSysCasePromptByCasePromptIds(ids));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }
}
