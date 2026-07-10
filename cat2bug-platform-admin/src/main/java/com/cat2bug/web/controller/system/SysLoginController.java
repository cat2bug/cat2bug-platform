package com.cat2bug.web.controller.system;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.system.domain.SysTeam;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.service.ISysTeamService;
import com.cat2bug.system.service.ISysUserConfigService;
import com.cat2bug.system.support.TeamMembershipSupport;

import java.util.HashMap;
import java.util.Map;
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
import com.cat2bug.web.service.OllamaAvailability;
import com.cat2bug.web.service.OllamaMenuFilter;

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
    private OllamaAvailability ollamaAvailability;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserConfigService userConfigService;

    @Autowired
    private TeamMembershipSupport teamMembershipSupport;

    @Autowired
    private ISysTeamService sysTeamService;

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

        Long currentTeamId = sysUserConfig != null ? sysUserConfig.getCurrentTeamId() : null;
        boolean teamMember = currentTeamId != null && currentTeamId > 0
                && teamMembershipSupport.isTeamMember(user.getUserId(), currentTeamId);

        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles.stream().map(r->r.getRoleKey()).collect(Collectors.toList()));
        ajax.put("permissions", permissions);
        ajax.put("config", sysUserConfig);
        ajax.put("teamMember", teamMember);
        if (!teamMember && currentTeamId != null && currentTeamId > 0) {
            SysTeam contextTeam = sysTeamService.selectSysTeamByTeamId(currentTeamId);
            if (contextTeam != null) {
                Map<String, Object> context = new HashMap<>();
                context.put("teamId", contextTeam.getTeamId());
                context.put("teamName", contextTeam.getTeamName());
                context.put("teamIcon", contextTeam.getTeamIcon());
                ajax.put("contextTeam", context);
            }
        }
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
        if (!ollamaAvailability.isAvailable())
        {
            menus = OllamaMenuFilter.removeOllamaMenus(menus);
        }
        return AjaxResult.success(menuService.buildMenus(menus));
    }
}
