package com.cat2bug.web.controller.login;

import com.cat2bug.common.constant.Constants;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginBody;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.framework.web.service.SysLoginService;
import com.cat2bug.framework.web.service.SysPermissionService;
import com.cat2bug.framework.web.service.WechatMpLoginService;
import com.cat2bug.system.service.ISysMenuService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.google.common.base.Preconditions;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 微信小程序登录验证
 * 
 * @author ruoyi
 */
@Log4j2
@RestController
@RequestMapping("/wechat-mp")
public class WechatMpLoginController
{
    @Autowired
    private WechatMpLoginService wechatMpLoginService;

    @Autowired
    private SysLoginService sysLoginService;


    /**
     * 微信小程序code登陆
     * @return
     */
    @PostMapping("/login")
    public AjaxResult wechatMpLogin(@RequestBody Map<String,Object> body) {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = wechatMpLoginService.selectUserByWechatMp(String.valueOf(body.get("code")));
        String token = wechatMpLoginService.login(user.getUserName());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /* 密码登陆
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/bind")
    public AjaxResult bind(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        sysLoginService.login(loginBody.getUsername(), loginBody.getPassword(), null, null);
        // 生成令牌
        boolean isBind = wechatMpLoginService.bind(loginBody.getUsername(), loginBody.getCode());
        Preconditions.checkArgument(isBind, "绑定微信账号失败!");
        return ajax;
    }
}
