package com.cat2bug.admin.resource.open;

import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.open.ApiMember;
import com.cat2bug.system.open.OpenApiContextService;
import com.cat2bug.system.open.OpenApiMemberService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/member")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiMemberResource {

    @Inject
    OpenApiContextService openApiContextService;

    @Inject
    OpenApiMemberService openApiMemberService;

    @GET
    public TableDataInfo list(@QueryParam("memberAccount") String memberAccount,
                              @QueryParam("memberName") String memberName) {
        Long projectId = openApiContextService.requireProjectId();
        ApiMember filter = new ApiMember();
        filter.setMemberAccount(memberAccount);
        filter.setMemberName(memberName);
        var rows = openApiMemberService.selectMemberList(projectId, filter);
        return new TableDataInfo(rows, rows.size());
    }
}
