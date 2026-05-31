package com.cat2bug.framework.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.cat2bug.common.constant.Constants;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.RegisterBody;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.manager.AsyncManager;
import com.cat2bug.framework.manager.factory.AsyncFactory;
import com.cat2bug.system.service.ISysUserService;

/**
 * 注册校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysRegisterService
{
    @Autowired
    private ISysUserService userService;

    /**
     * 注册
     */
    public SysUser register(RegisterBody registerBody)
    {
        String msg = "";
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        String nickName = registerBody.getNickName();
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setNickName(nickName);
        sysUser.setPassword(SecurityUtils.encryptPassword(password));

        if (StringUtils.isEmpty(username))
        {
            msg = MessageUtils.message("user.name.not_empty");
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = MessageUtils.message("user.password.not_empty");
        }
        else if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            msg = MessageUtils.message("user.account.size.exception");
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = MessageUtils.message("user.password.size.exception");
        }
        else if (!userService.checkUserNameUnique(sysUser))
        {
            msg = MessageUtils.message("user.register.fail.username-exists", username);
        }
        else
        {
            boolean regFlag = userService.registerUser(sysUser);
            if (!regFlag)
            {
                msg = MessageUtils.message("user.register.fail");
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
            }
        }

        if(StringUtils.isNotBlank(msg)) {
            throw new RuntimeException(msg);
        }
        return sysUser;
    }
}
