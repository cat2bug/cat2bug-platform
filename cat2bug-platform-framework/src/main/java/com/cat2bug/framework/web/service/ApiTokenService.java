package com.cat2bug.framework.web.service;

import com.cat2bug.common.constant.Constants;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.exception.user.UserPasswordNotMatchException;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.security.context.AuthenticationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-10 14:28
 * @Version: 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "cat2bug.api", name = "enabled", havingValue = "true")
public class ApiTokenService {
    private static final Logger log = LoggerFactory.getLogger(ApiTokenService.class);

    private static final String AUTH_TOKEN_HEADER_NAME = "CAT2BUG-API-KEY";

    private static final String API_CACHE_FLAG = "api_";

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认30分钟）
    @Value("${token.expireTime}")
    private int expireTime;

    @Resource(name="apiAuthenticationManager")
    private AuthenticationManager apiAuthenticationManager;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TEN = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    /**
     * 获取API用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                LoginUser user = redisCache.getCacheObject(RedisCache.API_TOKEN_CACHE_REGION, API_CACHE_FLAG+token);
                if(user==null){
                    // 用户验证
                    Authentication authentication = null;
                    try
                    {
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(token, "");
                        AuthenticationContextHolder.setContext(authenticationToken);
                        // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
                        authentication = this.apiAuthenticationManager.authenticate(authenticationToken);
                    }
                    catch (Exception e)
                    {
                        if (e instanceof BadCredentialsException)
                        {
                            throw new UserPasswordNotMatchException();
                        }
                        else
                        {
                            throw new ServiceException(e.getMessage());
                        }
                    }
                    finally
                    {
                        AuthenticationContextHolder.clearContext();
                    }
                    user = (LoginUser) authentication.getPrincipal();
                }

                return user;
            }
            catch (Exception e)
            {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 获取Api接口的token
     * @param request
     * @return
     */
    public String getToken(HttpServletRequest request) {
        String token = request.getHeader(AUTH_TOKEN_HEADER_NAME);
        if (StringUtils.isNotEmpty(token))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = API_CACHE_FLAG+loginUser.getToken();
        redisCache.setCacheObject("tokenExpireTime", userKey, loginUser);
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     *
     * @param loginUser
     * @return 令牌
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN)
        {
            refreshToken(loginUser);
        }
    }
}
