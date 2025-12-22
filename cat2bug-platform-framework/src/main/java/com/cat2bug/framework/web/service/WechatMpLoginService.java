package com.cat2bug.framework.web.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.common.constant.Constants;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.enums.UserStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.exception.user.*;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.ip.IpUtils;
import com.cat2bug.framework.manager.AsyncManager;
import com.cat2bug.framework.manager.factory.AsyncFactory;
import com.cat2bug.framework.security.context.AuthenticationContextHolder;
import com.cat2bug.system.exception.WechatMpNotBindException;
import com.cat2bug.system.service.ISysConfigService;
import com.cat2bug.system.service.ISysUserService;
import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 微信小程序登录校验方法
 * 
 * @author yuzhantao
 */
@Component
public class WechatMpLoginService
{
    private Logger log = LogManager.getLogger(WechatMpLoginService.class);

    /** 微信小程序登陆缓存组名 */
    private final static String WECHAT_MP_LOGIN_CACHE = "wechat-mp-login-cache";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private SysLoginService sysLoginService;

    @Value("${cat2bug.login.wechat-mp.appid}")
    private String wechatMpAppid;

    @Value("${cat2bug.login.wechat-mp.secret}")
    private String wechatMpSecret;

    /**
     * 根据微信小程序OpenId查找用户
     * @param code  小程序返回的code
     * @return      结果
     */
    public SysUser selectUserByWechatMp(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session"
                + "?appid=" + this.wechatMpAppid
                + "&secret=" + this.wechatMpSecret
                + "&js_code=" + code
                + "&grant_type=authorization_code";
        String json = HttpUtil.get(url);
        Map<String, Object> wechatResult = JSON.toJavaObject(json, Map.class);
        String openId = String.valueOf(wechatResult.get("openid"));
        String sessionKey = String.valueOf(wechatResult.get("session_key"));
        redisCache.setCacheObject(WECHAT_MP_LOGIN_CACHE, code,  wechatResult.get("openid"));
        SysUser user = userService.selectUserByWechatMp(openId);
        if(user==null) {
            throw new WechatMpNotBindException(sessionKey);
        }
        return user;
    }

    /**
     * 绑定用户
     * @param username  用户名
     * @param code    缓存
     * @return          是否绑定成功
     */
    public boolean bind(String username, String code) {
        Preconditions.checkNotNull(code, "未识别到用户微信账号，请刷新重试!");
        Preconditions.checkArgument(redisCache.hasKey(WECHAT_MP_LOGIN_CACHE, code), "未找到用户微信账号，请刷新重试!");
        String openId = this.redisCache.getCacheObject(WECHAT_MP_LOGIN_CACHE, code);
        return userService.updateWechatMp(username, openId);
    }

    /**
     * 登录验证
     * @param username 用户名
     * @return 结果
     */
    public String login(String username) {
        SysUser user = userService.selectUserByUserName(username);
        if (user == null)
        {
            AsyncManager.me().execute(
                    AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, "用户不存在")
            );
            throw new ServiceException("用户不存在");
        }
        // 3. 用户状态校验
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            throw new ServiceException("用户已停用");
        }
        // 4. 构造 LoginUser
        LoginUser loginUser = (LoginUser)userDetailsService.createLoginUser(user);
        // 5. 记录登录信息
        AsyncManager.me().execute(
                AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, "登录成功")
        );
        sysLoginService.recordLoginInfo(user.getUserId());
        // 6. 生成 token
        return tokenService.createToken(loginUser);
    }
}
