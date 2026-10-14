package com.cat2bug.admin.resource;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.system.service.SysMemberInviteService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

/**
 * 成员邀请链接公开接口
 */
@Path("/invite")
@Produces(MediaType.APPLICATION_JSON)
public class InviteResource {

    @Inject
    AuthContext authContext;

    @Inject
    SysMemberInviteService sysMemberInviteService;

    @GET
    @Path("/{token}/preview")
    public AjaxResult preview(@PathParam("token") String token) {
        return AjaxResult.success(sysMemberInviteService.preview(token, resolveOptionalUserId()));
    }

    /**
     * 兼容旧版 /invite/{token} 直链：重定向到 hash 路由邀请页。
     */
    @GET
    @Path("/{token}")
    @Produces(MediaType.WILDCARD)
    public Response openInvitePage(@PathParam("token") String token) {
        return Response.temporaryRedirect(URI.create("/#/invite/" + token)).build();
    }

    @POST
    @Path("/{token}/accept")
    public AjaxResult accept(@PathParam("token") String token) {
        LoginUser loginUser = requireLoginUser();
        return AjaxResult.success(sysMemberInviteService.accept(
                token, loginUser.getUserId(), String.valueOf(loginUser.getUserId())));
    }

    private Long resolveOptionalUserId() {
        LoginUser loginUser = authContext.getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            throw new ApiException(401, "未登录或会话已失效");
        }
        return loginUser;
    }
}
