package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.LoginLogService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.framework.security.AuthContext;
import com.cat2bug.framework.security.TokenService;
import com.cat2bug.system.mapper.SysUserMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON)
public class LogoutResource {

    static final String MSG_LOGOUT = "退出成功";

    @Inject
    TokenService tokenService;

    @Inject
    AuthContext authContext;

    @Inject
    SysUserMapper sysUserMapper;

    @Inject
    LoginLogService loginLogService;

    @POST
    public AjaxResult logout(@Context HttpHeaders headers) {
        LoginUser loginUser = authContext.getLoginUser();
        if (loginUser == null) {
            String sessionId = tokenService.sessionIdFromAuthorization(headers.getHeaderString("Authorization"));
            loginUser = tokenService.getLoginUser(headers.getHeaderString("Authorization"));
            tokenService.delLoginUser(sessionId);
        } else {
            tokenService.delLoginUser(loginUser.getToken());
        }
        if (loginUser != null && loginUser.getUserId() != null) {
            sysUserMapper.clearLoginInfo(loginUser.getUserId());
            String userName = loginUser.getUser() != null ? loginUser.getUser().getUserName() : null;
            loginLogService.record(userName, LoginLogService.SUCCESS, MSG_LOGOUT);
        }
        return AjaxResult.success(MSG_LOGOUT);
    }
}
