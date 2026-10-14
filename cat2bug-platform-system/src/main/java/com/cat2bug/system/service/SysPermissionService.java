package com.cat2bug.system.service;

import com.cat2bug.common.constant.SecurityConstants;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.security.TokenService;
import com.cat2bug.system.mapper.SysMenuMapper;
import com.cat2bug.system.mapper.SysUserConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SysPermissionService {

    @Inject
    SysMenuMapper menuMapper;

    @Inject
    SysUserConfigMapper userConfigMapper;

    @Inject
    SysRoleService roleService;

    @Inject
    TokenService tokenService;

    public void updateRoleAndPermissionOfCurrentUser(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUser() == null) {
            return;
        }
        SysUser user = loginUser.getUser();
        Set<SysRole> roles = getRole(user).stream()
                .filter(SysRole::isFlag)
                .collect(Collectors.toSet());
        user.setRoleIds(roles.stream().map(SysRole::getRoleId).toArray(Long[]::new));
        user.setRoles(new ArrayList<>(roles));
        loginUser.setPermissions(getMenuPermission(user));
        loginUser.setContextPermissionsMerged(true);
        tokenService.updateLoginUser(loginUser);
    }

    public Set<String> getMenuPermission(SysUser user) {
        Set<String> perms = new HashSet<>();
        if (user.isAdmin()) {
            perms.add(SecurityConstants.ALL_PERMISSION);
            return perms;
        }
        List<SysRole> roles = user.getRoles();
        if (roles != null && !roles.isEmpty()) {
            for (SysRole role : roles) {
                if (!role.isFlag()) {
                    continue;
                }
                addPermsFromRole(role.getRoleId(), perms);
            }
        } else {
            addRawPerms(menuMapper.selectMenuPermsByUserId(user.getUserId()), perms);
        }
        return perms;
    }

    public List<String> getRoleKeys(SysUser user) {
        if (user.isAdmin()) {
            return List.of(SecurityConstants.SUPER_ADMIN);
        }
        return roleService.selectRolePermissionByUserId(user.getUserId()).stream().collect(Collectors.toList());
    }

    public LoginUser createLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUser(user);

        Set<SysRole> roles = getRole(user).stream()
                .filter(SysRole::isFlag)
                .collect(Collectors.toSet());
        user.setRoleIds(roles.stream().map(SysRole::getRoleId).toArray(Long[]::new));
        user.setRoles(new ArrayList<>(roles));
        loginUser.setPermissions(getMenuPermission(user));
        loginUser.setContextPermissionsMerged(true);
        return loginUser;
    }

    public Set<SysRole> getRole(SysUser user) {
        Set<SysRole> roles = new HashSet<>();
        if (user.isAdmin()) {
            SysRole adminRole = roleService.selectRoleById(1L);
            if (adminRole != null) {
                adminRole.setFlag(true);
                roles.add(adminRole);
            }
        } else {
            roles.addAll(roleService.selectRolesByUserId(user.getUserId()));
        }
        return roles;
    }

    public SysUserConfig getUserConfig(Long userId) {
        return userConfigMapper.selectSysUserConfigByUserId(userId);
    }

    private void addPermsFromRole(Long roleId, Set<String> perms) {
        addRawPerms(menuMapper.selectMenuPermsByRoleId(roleId), perms);
    }

    private static void addRawPerms(List<String> raw, Set<String> perms) {
        if (raw == null) {
            return;
        }
        for (String perm : raw) {
            if (StringUtils.isNotEmpty(perm)) {
                perms.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
    }
}
