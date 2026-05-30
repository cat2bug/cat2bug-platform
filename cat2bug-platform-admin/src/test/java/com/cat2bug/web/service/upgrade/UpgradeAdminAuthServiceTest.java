package com.cat2bug.web.service.upgrade;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.system.service.ISysUserService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpgradeAdminAuthServiceTest
{
    @Mock
    private ISysUserService userService;

    @InjectMocks
    private UpgradeAdminAuthService upgradeAdminAuthService;

    @Test
    void verifyAdministrator_acceptsValidSystemAdmin()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setAdminUsername("admin");
        request.setAdminPassword("cat2bug");

        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setStatus("0");
        user.setPassword(SecurityUtils.encryptPassword("cat2bug"));
        SysRole role = new SysRole();
        role.setRoleId(1L);
        role.setRoleKey("admin");
        user.setRoles(List.of(role));

        when(userService.selectUserByUserName("admin")).thenReturn(user);

        assertDoesNotThrow(() -> upgradeAdminAuthService.verifyAdministrator(request));
    }

    @Test
    void verifyAdministrator_rejectsWrongPassword()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setAdminUsername("admin");
        request.setAdminPassword("wrong");

        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setStatus("0");
        user.setPassword(SecurityUtils.encryptPassword("cat2bug"));
        user.setRoles(List.of());

        when(userService.selectUserByUserName("admin")).thenReturn(user);

        Exception ex = assertThrows(Exception.class, () -> upgradeAdminAuthService.verifyAdministrator(request));
        assertNotNull(ex.getMessage());
    }

    @Test
    void verifyAdministrator_rejectsNonAdminUser()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setAdminUsername("demo");
        request.setAdminPassword("123456");

        SysUser user = new SysUser();
        user.setUserId(2L);
        user.setUserName("demo");
        user.setStatus("0");
        user.setPassword(SecurityUtils.encryptPassword("123456"));
        user.setRoles(List.of());

        when(userService.selectUserByUserName("demo")).thenReturn(user);

        assertThrows(Exception.class, () -> upgradeAdminAuthService.verifyAdministrator(request));
    }

    @Test
    void verifyAdministrator_requiresCredentials()
    {
        assertThrows(Exception.class, () -> upgradeAdminAuthService.verifyAdministrator(null));
        assertThrows(Exception.class, () -> upgradeAdminAuthService.verifyAdministrator(new SetupSubmitRequest()));
    }
}
