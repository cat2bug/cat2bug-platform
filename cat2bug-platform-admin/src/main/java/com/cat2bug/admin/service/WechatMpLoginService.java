package com.cat2bug.admin.service;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.exception.WechatMpNotBindException;
import com.cat2bug.framework.cache.AppCache;
import com.cat2bug.framework.security.TokenService;
import com.cat2bug.system.service.SysPermissionService;
import com.cat2bug.system.service.SysUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@ApplicationScoped
public class WechatMpLoginService {

    private static final String WECHAT_MP_LOGIN_CACHE = "wechat-mp-login-cache";

    @ConfigProperty(name = "cat2bug.login.wechat-mp.appid")
    java.util.Optional<String> wechatMpAppid;

    @ConfigProperty(name = "cat2bug.login.wechat-mp.secret")
    java.util.Optional<String> wechatMpSecret;

    @Inject
    AppCache appCache;

    @Inject
    SysUserService sysUserService;

    @Inject
    SysPermissionService sysPermissionService;

    @Inject
    TokenService tokenService;

    @Inject
    LoginLogService loginLogService;

    @Inject
    ObjectMapper objectMapper;

    public SysUser selectUserByWechatMp(String code) {
        Map<String, Object> wechatResult = fetchWechatSession(code);
        String openId = stringValue(wechatResult.get("openid"));
        String sessionKey = stringValue(wechatResult.get("session_key"));
        if (openId.isBlank()) {
            throw new ApiException("微信小程序登录失败：" + stringValue(wechatResult.get("errmsg")));
        }
        appCache.set(WECHAT_MP_LOGIN_CACHE, code, openId);
        SysUser user = sysUserService.selectUserByWechatMp(openId);
        if (user == null) {
            throw new WechatMpNotBindException(sessionKey);
        }
        return user;
    }

    public boolean bind(String username, String code) {
        if (code == null || code.isBlank()) {
            throw new ApiException("未识别到用户微信账号，请刷新重试!");
        }
        String openId = appCache.get(WECHAT_MP_LOGIN_CACHE, code, String.class);
        if (openId == null || openId.isBlank()) {
            throw new ApiException("未找到用户微信账号，请刷新重试!");
        }
        return sysUserService.updateWechatMp(username, openId);
    }

    public String login(String username) {
        SysUser user = sysUserService.selectUserByUserName(username);
        if (user == null) {
            loginLogService.record(username, LoginLogService.FAIL, "用户不存在");
            throw new ApiException("用户不存在");
        }
        if ("1".equals(user.getStatus())) {
            throw new ApiException("用户已停用");
        }
        LoginUser loginUser = sysPermissionService.createLoginUser(user);
        loginLogService.record(username, LoginLogService.SUCCESS, "登录成功");
        loginLogService.recordLoginInfo(user.getUserId());
        return tokenService.createToken(loginUser);
    }

    private Map<String, Object> fetchWechatSession(String code) {
        String appid = wechatMpAppid.orElse("").trim();
        String secret = wechatMpSecret.orElse("").trim();
        if (appid.isBlank() || secret.isBlank()) {
            throw new ApiException("未配置微信小程序 appid/secret");
        }
        String query = "appid=" + encode(appid)
                + "&secret=" + encode(secret)
                + "&js_code=" + encode(code)
                + "&grant_type=authorization_code";
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.weixin.qq.com/sns/jscode2session?" + query))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new ApiException("调用微信登录接口失败: " + e.getMessage());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
