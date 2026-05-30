package com.cat2bug.web.service.upgrade;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.enums.UserStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.service.ISysUserService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import com.cat2bug.web.service.setup.SetupMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 升级向导：校验系统管理员账号密码，防止未授权执行迁移。
 */
@Service
public class UpgradeAdminAuthService
{
    private static final String ADMIN_ROLE_KEY = "admin";

    @Autowired
    private ISysUserService userService;

    public void verifyAdministrator(SetupSubmitRequest request)
    {
        if (request == null)
        {
            throw new ServiceException(SetupMessages.msg("upgrade.auth.required"));
        }
        String username = StringUtils.trim(request.getAdminUsername());
        String password = request.getAdminPassword();
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            throw new ServiceException(SetupMessages.msg("upgrade.auth.required"));
        }

        SysUser user = userService.selectUserByUserName(username);
        if (user == null)
        {
            throw new ServiceException(SetupMessages.msg("upgrade.auth.denied"));
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            throw new ServiceException(SetupMessages.msg("upgrade.auth.denied"));
        }
        if (!isSystemAdministrator(user))
        {
            throw new ServiceException(SetupMessages.msg("upgrade.auth.not.admin"));
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            throw new ServiceException(SetupMessages.msg("upgrade.auth.denied"));
        }
    }

    private static boolean isSystemAdministrator(SysUser user)
    {
        if (user == null || user.getUserId() == null)
        {
            return false;
        }
        if (SecurityUtils.isAdmin(user.getUserId()))
        {
            return true;
        }
        List<SysRole> roles = user.getRoles();
        if (roles == null || roles.isEmpty())
        {
            return false;
        }
        return roles.stream().anyMatch(role ->
                role != null
                        && (ADMIN_ROLE_KEY.equals(role.getRoleKey())
                        || Long.valueOf(1L).equals(role.getRoleId())));
    }
}
