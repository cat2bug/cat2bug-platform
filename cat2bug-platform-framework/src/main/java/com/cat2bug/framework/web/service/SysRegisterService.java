package com.cat2bug.framework.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.common.constant.Constants;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.RegisterBody;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.exception.user.CaptchaException;
import com.cat2bug.common.exception.user.CaptchaExpireException;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.manager.AsyncManager;
import com.cat2bug.framework.manager.factory.AsyncFactory;
import com.cat2bug.system.service.ISysConfigService;
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

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 注册
     */
    public String register(RegisterBody registerBody)
    {
        String msg = "";
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        String phoneNumber = registerBody.getPhoneNumber();
        String nickName = registerBody.getNickName();
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);
        sysUser.setPhoneNumber(phoneNumber);
        sysUser.setNickName(nickName);
        sysUser.setPassword(SecurityUtils.encryptPassword(password));

        // 验证码开关
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isEmpty(username))
        {
            msg = MessageUtils.message("user.name.not_empty");
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = MessageUtils.message("user.password.not_empty");
        }
        else if(StringUtils.isEmpty(phoneNumber))
        {
            msg = MessageUtils.message("user.phone_number.not_empty");
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
        else if(!userService.checkPhoneUnique(sysUser)) {
            msg = MessageUtils.message("user.register.fail.phone-exists", username);
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
        return msg;
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(RedisCache.VERIFY_CODE_CACHE_REGION, verifyKey);
        redisCache.deleteObject(RedisCache.VERIFY_CODE_CACHE_REGION, verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
