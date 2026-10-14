package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.SysCasePrompt;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.SysCasePromptService;
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

@Path("/system/CasePrompt")
@Produces(MediaType.APPLICATION_JSON)
public class CasePromptResource {

    @Inject
    SysCasePromptService sysCasePromptService;

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
