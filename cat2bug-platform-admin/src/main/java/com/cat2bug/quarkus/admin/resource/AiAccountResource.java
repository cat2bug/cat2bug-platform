package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.domain.AiAccount;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.framework.security.RequiresPermissions;
import com.cat2bug.quarkus.system.service.AiAccountService;
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

@Path("/system/ai/account")
@Produces(MediaType.APPLICATION_JSON)
public class AiAccountResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    AuthContext authContext;

    @Inject
    AiAccountService aiAccountService;

    @Inject
    SystemExcelExportService systemExcelExportService;

    @GET
    @Path("/list")
    @RequiresPermissions(value = {"ai:account:list", "system:case:add"},
            logical = RequiresPermissions.Logical.OR)
    public TableDataInfo list(@QueryParam("modelName") String modelName,
                              @QueryParam("projectId") Long projectId,
                              @QueryParam("accountName") String accountName,
                              @QueryParam("pageNum") Integer pageNum,
                              @QueryParam("pageSize") Integer pageSize) {
        AiAccount query = new AiAccount();
        query.setModelName(modelName);
        query.setProjectId(projectId);
        query.setAccountName(accountName);
        return aiAccountService.selectAiAccountPage(query, pageNum, pageSize);
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(SPREADSHEET_MEDIA_TYPE)
    @RequiresPermissions("ai:account:export")
    public Response export(@RestForm("modelName") String modelName,
                           @RestForm("projectId") Long projectId,
                           @RestForm("accountName") String accountName) {
        AiAccount query = new AiAccount();
        query.setModelName(modelName);
        query.setProjectId(projectId);
        query.setAccountName(accountName);
        byte[] workbook = systemExcelExportService.exportAiAccounts(query);
        return Response.ok(workbook).type(SPREADSHEET_MEDIA_TYPE).build();
    }

    @GET
    @Path("/{accountId}")
    @RequiresPermissions("ai:account:query")
    public AjaxResult getInfo(@PathParam("accountId") Long accountId) {
        return AjaxResult.success(aiAccountService.selectAiAccountByAccountId(accountId));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("ai:account:add")
    public AjaxResult add(AiAccount aiAccount) {
        aiAccount.setCreateBy(requireLoginUser().getUserId());
        return toAjax(aiAccountService.insertAiAccount(aiAccount));
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresPermissions("ai:account:edit")
    public AjaxResult edit(AiAccount aiAccount) {
        return toAjax(aiAccountService.updateAiAccount(aiAccount));
    }

    @DELETE
    @Path("/{accountIds}")
    @RequiresPermissions("ai:account:remove")
    public AjaxResult remove(@PathParam("accountIds") String accountIds) {
        Long[] ids = Arrays.stream(accountIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toArray(Long[]::new);
        return toAjax(aiAccountService.deleteAiAccountByAccountIds(ids));
    }

    @POST
    @Path("/test/{accountId}")
    @RequiresPermissions("ai:account:edit")
    public AjaxResult testConnection(@PathParam("accountId") Long accountId) {
        return AjaxResult.success(aiAccountService.testOpenAiConnection(accountId));
    }

    private AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error("操作失败");
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException("未登录");
        }
        return loginUser;
    }
}
