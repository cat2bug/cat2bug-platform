package com.cat2bug.web.controller.system;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysUserConfigService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.cat2bug.common.constant.Constants;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.domain.entity.SysMenu;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginBody;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.framework.web.service.SysLoginService;
import com.cat2bug.framework.web.service.SysPermissionService;
import com.cat2bug.system.service.ISysMenuService;

/**
 * 登录验证
 * 
 * @author ruoyi
 */
@Log4j2
@RestController
public class SysLoginController
{
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserConfigService userConfigService;

    /**
     * 登录方法
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        // 更新用户权限
//        permissionService.updateRoleAndPermissionOfCurrentUser();
        SysUser user = SecurityUtils.getLoginUser().getUser();

        // 角色集合
        Set<SysRole> roles = permissionService.getRole(user).stream().filter(r->r.isFlag()).collect(Collectors.toSet());
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);

        SysUserConfig sysUserConfig = userConfigService.selectSysUserConfigByCurrentUserId();

        log.info("=======getInfo:::roles={}   permissions={}   sysUserConfig={}", JSON.toJSONString(roles), JSON.toJSONString(permissions),JSON.toJSONString(sysUserConfig));
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles.stream().map(r->r.getRoleKey()).collect(Collectors.toList()));
        ajax.put("permissions", permissions);
        ajax.put("config", sysUserConfig);
        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
