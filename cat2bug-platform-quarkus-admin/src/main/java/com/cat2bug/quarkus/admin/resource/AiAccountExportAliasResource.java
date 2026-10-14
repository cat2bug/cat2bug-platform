package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.SystemExcelExportService;
import com.cat2bug.quarkus.common.domain.AiAccount;
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
 * 前端 OpenAI 账号页使用 {@code ai/account/export}（与 {@link AiAccountResource} 等价）。
 */
@Path("/ai/account")
public class AiAccountExportAliasResource {

    static final String SPREADSHEET_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Inject
    SystemExcelExportService systemExcelExportService;

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
}
