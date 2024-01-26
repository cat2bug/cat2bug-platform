package com.cat2bug.framework.web.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.domain.SysUserConfig;
import com.cat2bug.system.domain.SysUserProjectRole;
import com.cat2bug.system.service.*;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;

/**
 * 用户权限处理
 * 
 * @author ruoyi
 */
@Component
@Log4j2
public class SysPermissionService
{
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysUserProjectRoleService sysUserProjectRoleService;

    @Autowired
    private ISysUserTeamRoleService sysUserTeamRoleService;

    @Autowired
    private ISysUserConfigService sysUserConfigService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private TokenService tokenService;
    /**
     * 更新当前用户权限
     */
    public void updateRoleAndPermissionOfCurrentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<SysRole> roles = this.getRole(user).stream().filter(r->r.isFlag()).collect(Collectors.toSet());
        // 刷新角色
        user.setRoleIds(roles.stream().map(r->r.getRoleId()).collect(Collectors.toList()).toArray(new Long[]{}));
        user.setRoles(roles.stream().collect(Collectors.toList()));
        // 权限集合
        Set<String> permissions = this.getMenuPermission(user);
        // 刷新权限
        loginUser.setPermissions(permissions);

        tokenService.setLoginUser(loginUser);
    }

    /**
     * 获取角色数据权限
     * 
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user)
    {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            roles.add("admin");
        }
        else
        {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    public Set<SysRole> getRole(SysUser user) {
        Set<SysRole> roles = new HashSet<>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            SysRole adminRole = roleService.selectRoleById(1L);
            adminRole.setFlag(true);
            roles.add(adminRole);
        }
        else
        {
            roles.addAll(roleService.selectRolesByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     * 
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(SysUser user)
    {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            perms.add("*:*:*");
        }
        else
        {
            List<SysRole> roles = user.getRoles();
            if (!CollectionUtils.isEmpty(roles))
            {
                // 多角色设置permissions属性，以便数据权限匹配权限
                for (SysRole role : roles)
                {
                    Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                    role.setPermissions(rolePerms);
                    perms.addAll(rolePerms);
                }
            }
            else
            {
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
        }
        return perms;
    }
}
