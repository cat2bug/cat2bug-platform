package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.util.InviteLinkUrlSupport;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.vo.CreateMemberInviteVo;
import com.cat2bug.quarkus.common.domain.vo.MemberInviteAcceptResultVo;
import com.cat2bug.quarkus.common.domain.vo.MemberInvitePreviewVo;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.AuthContext;
import com.cat2bug.quarkus.system.service.SysMemberInviteService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("/invite")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InviteResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysMemberInviteService sysMemberInviteService;

    @GET
    @Path("/{token}/preview")
    public AjaxResult preview(@PathParam("token") String token) {
        MemberInvitePreviewVo preview = sysMemberInviteService.preview(token);
        return AjaxResult.success(preview);
    }

    @POST
    @Path("/{token}/accept")
    public AjaxResult accept(@PathParam("token") String token) {
        LoginUser loginUser = requireLoginUser();
        MemberInviteAcceptResultVo result = sysMemberInviteService.accept(
                token, loginUser.getUserId(), String.valueOf(loginUser.getUserId()));
        return AjaxResult.success(result);
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
