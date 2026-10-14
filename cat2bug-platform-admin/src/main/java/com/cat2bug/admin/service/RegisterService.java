package com.cat2bug.admin.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.common.api.RegisterBody;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.SysUserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RegisterService {

    @Inject
    SysUserService sysUserService;

    @Inject
    LoginLogService loginLogService;

    public void register(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        String nickName = registerBody.getNickName();
        String msg = "";

        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(StringUtils.isNotEmpty(nickName) ? nickName : username);
        sysUser.setPassword(BCrypt.withDefaults().hashToString(12, password.toCharArray()));

        if (StringUtils.isEmpty(username)) {
            msg = "成员名不能为空";
        } else if (StringUtils.isEmpty(password)) {
            msg = "成员密码不能为空";
        } else if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH) {
            msg = "账户长度必须在2到20个字符之间";
        } else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            msg = "密码长度必须在5到20个字符之间";
        } else if (!sysUserService.checkUserNameUnique(sysUser)) {
            msg = "保存成员[" + username + "]失败，注册账号已存在";
        } else if (!sysUserService.registerUser(sysUser)) {
            msg = "注册成员失败，请联系系统管理人员";
        }

        if (StringUtils.isNotEmpty(msg)) {
            throw new RuntimeException(msg);
        }
        loginLogService.record(username, LoginLogService.SUCCESS, "注册成功");
    }
}
