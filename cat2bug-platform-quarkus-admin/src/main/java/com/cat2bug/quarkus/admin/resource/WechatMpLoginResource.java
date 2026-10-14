package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.admin.service.AuthService;
import com.cat2bug.quarkus.admin.service.LoginLogService;
import com.cat2bug.quarkus.admin.service.WechatMpLoginService;
import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.api.LoginBody;
import com.cat2bug.quarkus.common.domain.SysUser;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/wechat-mp")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WechatMpLoginResource {

    @Inject
    WechatMpLoginService wechatMpLoginService;

    @Inject
    AuthService authService;

    @Inject
    LoginLogService loginLogService;

    @POST
    @Path("/login")
    public AjaxResult wechatMpLogin(Map<String, Object> body) {
        Object code = body != null ? body.get("code") : null;
        if (code == null) {
            return AjaxResult.error("code 不能为空");
        }
        SysUser user = wechatMpLoginService.selectUserByWechatMp(String.valueOf(code));
        String token = wechatMpLoginService.login(user.getUserName());
        AjaxResult ajax = AjaxResult.success();
        ajax.put(AjaxResult.TOKEN_TAG, token);
        return ajax;
    }

    @POST
    @Path("/bind")
    public AjaxResult bind(LoginBody loginBody) {
        if (loginBody == null || loginBody.getUsername() == null || loginBody.getPassword() == null) {
            return AjaxResult.error("用户名或密码不能为空");
        }
        if (authService.authenticate(loginBody.getUsername(), loginBody.getPassword()) == null) {
            loginLogService.record(loginBody.getUsername(), LoginLogService.FAIL, LoginResource.MSG_NOT_MATCH);
            return AjaxResult.error(LoginResource.MSG_NOT_MATCH);
        }
        if (!wechatMpLoginService.bind(loginBody.getUsername(), loginBody.getCode())) {
            return AjaxResult.error("绑定微信账号失败");
        }
        return AjaxResult.success();
    }
}
