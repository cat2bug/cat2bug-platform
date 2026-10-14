package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.AuthService;
import com.cat2bug.quarkus.admin.service.CaptchaService;
import com.cat2bug.quarkus.admin.service.LoginLogService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.LoginBody;
import com.cat2bug.quarkus.common.constant.UserConstants;
import com.cat2bug.quarkus.common.domain.LoginUser;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.security.TokenService;
import com.cat2bug.quarkus.framework.web.ClientInfo;
import com.cat2bug.quarkus.system.service.SysConfigService;
import com.cat2bug.quarkus.system.service.SysPermissionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoginResource {

    static final String MSG_NOT_MATCH = "成员不存在/密码错误";
    static final String MSG_BLOCKED = "成员已封禁，请联系管理员";
    static final String MSG_IP_BLACKLIST = "很遗憾，访问IP已被列入系统黑名单";
    static final String MSG_EMPTY = "* 必须填写";
    static final String MSG_SUCCESS = "登录成功";

    @Inject
    AuthService authService;

    @Inject
    TokenService tokenService;

    @Inject
    SysPermissionService permissionService;

    @Inject
    CaptchaService captchaService;

    @Inject
    SysConfigService sysConfigService;

    @Inject
    LoginLogService loginLogService;

    @POST
    public AjaxResult login(LoginBody body) {
        String username = body != null ? body.getUsername() : null;
        String password = body != null ? body.getPassword() : null;
        try {
            captchaService.validateCaptcha(body != null ? body.getCode() : null, body != null ? body.getUuid() : null);
        } catch (ApiException e) {
            loginLogService.record(username, LoginLogService.FAIL, e.getMessage());
            throw e;
        }
        preCheck(username, password);
        SysUser user = authService.findUndeletedUser(username);
        if (user == null) {
            return fail(username, MSG_NOT_MATCH);
        }
        if ("1".equals(user.getStatus())) {
            return fail(username, MSG_BLOCKED);
        }
        if (!authService.passwordMatches(user, password)) {
            return fail(username, MSG_NOT_MATCH);
        }
        loginLogService.record(username, LoginLogService.SUCCESS, MSG_SUCCESS);
        loginLogService.recordLoginInfo(user.getUserId());
        LoginUser loginUser = permissionService.createLoginUser(user);
        String token = tokenService.createToken(loginUser);
        AjaxResult result = AjaxResult.success();
        result.put(AjaxResult.TOKEN_TAG, token);
        return result;
    }

    private void preCheck(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            loginLogService.record(username, LoginLogService.FAIL, MSG_EMPTY);
            throw new ApiException(MSG_NOT_MATCH);
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH
                || username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            loginLogService.record(username, LoginLogService.FAIL, MSG_NOT_MATCH);
            throw new ApiException(MSG_NOT_MATCH);
        }
        String blackList = sysConfigService.selectConfigByKey("sys.login.blackIPList");
        if (LoginLogService.isMatchedIp(blackList, ClientInfo.current().ip())) {
            loginLogService.record(username, LoginLogService.FAIL, MSG_IP_BLACKLIST);
            throw new ApiException(MSG_IP_BLACKLIST);
        }
    }

    private AjaxResult fail(String username, String message) {
        loginLogService.record(username, LoginLogService.FAIL, message);
        return AjaxResult.error(message);
    }
}
