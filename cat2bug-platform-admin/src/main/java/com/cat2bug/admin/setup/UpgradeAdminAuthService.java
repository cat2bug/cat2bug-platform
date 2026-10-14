package com.cat2bug.admin.setup;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.setup.SetupMessages;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.SysUserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 升级向导：校验系统管理员账号密码。
 */
@ApplicationScoped
public class UpgradeAdminAuthService {

    private static final String ADMIN_ROLE_KEY = "admin";

    @Inject
    SysUserMapper sysUserMapper;

    public void verifyAdministrator(SetupSubmitRequest request) {
        if (request == null) {
            throw new ApiException(SetupMessages.msg("upgrade.auth.required"));
        }
        String username = request.getAdminUsername() != null ? request.getAdminUsername().trim() : null;
        String password = request.getAdminPassword();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new ApiException(SetupMessages.msg("upgrade.auth.required"));
        }

        SysUser user = sysUserMapper.selectUserByUserName(username);
        if (user == null || !"0".equals(user.getStatus())) {
            throw new ApiException(SetupMessages.msg("upgrade.auth.denied"));
        }
        if (!isSystemAdministrator(user)) {
            throw new ApiException(SetupMessages.msg("upgrade.auth.not.admin"));
        }
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
        if (!result.verified) {
            throw new ApiException(SetupMessages.msg("upgrade.auth.denied"));
        }
    }

    private static boolean isSystemAdministrator(SysUser user) {
        if (user == null || user.getUserId() == null) {
            return false;
        }
        if (SysUser.isAdmin(user.getUserId())) {
            return true;
        }
        List<SysRole> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role ->
                role != null
                        && (ADMIN_ROLE_KEY.equals(role.getRoleKey())
                        || Long.valueOf(1L).equals(role.getRoleId())));
    }
}
