package com.cat2bug.system.security;

import com.cat2bug.common.constant.SecurityConstants;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserConfig;
import com.cat2bug.common.security.LoginUserPermissionEnricher;
import com.cat2bug.framework.security.TokenService;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.service.SysPermissionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class DefaultLoginUserPermissionEnricher implements LoginUserPermissionEnricher {

    @Inject
    SysPermissionService permissionService;

    @Inject
    SysUserMapper userMapper;

    @Inject
    TokenService tokenService;

    @Override
    public void enrichIfNeeded(LoginUser loginUser) {
        if (loginUser == null) {
            return;
        }
        if (SysUser.isAdmin(loginUser.getUserId())) {
            if (loginUser.getPermissions() == null || loginUser.getPermissions().isEmpty()) {
                Set<String> perms = new HashSet<>();
                perms.add(SecurityConstants.ALL_PERMISSION);
                loginUser.setPermissions(perms);
                loginUser.setContextPermissionsMerged(true);
                tokenService.updateLoginUser(loginUser);
            }
            return;
        }

        if (loginUser.isContextPermissionsMerged()) {
            return;
        }

        SysUser user = loginUser.getUser();
        if (user == null || user.getUserId() == null) {
            Long userId = loginUser.getUserId();
            if (userId != null) {
                user = userMapper.selectUserById(userId);
                loginUser.setUser(user);
            }
        }
        if (user == null) {
            return;
        }
        if (loginUser.getUserId() == null) {
            loginUser.setUserId(user.getUserId());
        }

        SysUserConfig config = permissionService.getUserConfig(loginUser.getUserId());
        if (config == null) {
            return;
        }
        boolean hasContext = (config.getCurrentTeamId() != null && config.getCurrentTeamId() > 0)
                || (config.getCurrentProjectId() != null && config.getCurrentProjectId() > 0);
        if (!hasContext && (loginUser.getPermissions() == null || loginUser.getPermissions().isEmpty())) {
            permissionService.updateRoleAndPermissionOfCurrentUser(loginUser);
            return;
        }
        if (hasContext) {
            permissionService.updateRoleAndPermissionOfCurrentUser(loginUser);
        }
    }
}
