package com.cat2bug.framework.web.service;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.RegisterBody;
import com.cat2bug.common.utils.spring.SpringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysRegisterService 注册逻辑单元测试（注册不校验验证码）。
 */
@ExtendWith(MockitoExtension.class)
class SysRegisterServiceTest
{
    @Mock
    private com.cat2bug.system.service.ISysUserService userService;

    @InjectMocks
    private SysRegisterService registerService;

    @Test
    void register_succeedsWithoutCaptchaCodeOrUuid()
    {
        RegisterBody body = validRegisterBody();
        body.setCode(null);
        body.setUuid(null);

        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.checkPhoneUnique(any(SysUser.class))).thenReturn(true);
        when(userService.registerUser(any(SysUser.class))).thenReturn(true);

        try (MockedStatic<SpringUtils> springUtils = mockStatic(SpringUtils.class))
        {
            MessageSource messageSource = mock(MessageSource.class);
            ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("ok");
            springUtils.when(() -> SpringUtils.getBean(MessageSource.class)).thenReturn(messageSource);
            springUtils.when(() -> SpringUtils.getBean("scheduledExecutorService")).thenReturn(executor);

            SysUser result = registerService.register(body);

            assertNotNull(result);
            verify(userService).registerUser(any(SysUser.class));
        }
    }

    @Test
    void register_ignoresInvalidCaptchaFields()
    {
        RegisterBody body = validRegisterBody();
        body.setCode("wrong-code");
        body.setUuid("stale-uuid");

        when(userService.checkUserNameUnique(any(SysUser.class))).thenReturn(true);
        when(userService.checkPhoneUnique(any(SysUser.class))).thenReturn(true);
        when(userService.registerUser(any(SysUser.class))).thenReturn(true);

        try (MockedStatic<SpringUtils> springUtils = mockStatic(SpringUtils.class))
        {
            MessageSource messageSource = mock(MessageSource.class);
            ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
            when(messageSource.getMessage(anyString(), any(), any())).thenReturn("ok");
            springUtils.when(() -> SpringUtils.getBean(MessageSource.class)).thenReturn(messageSource);
            springUtils.when(() -> SpringUtils.getBean("scheduledExecutorService")).thenReturn(executor);

            assertDoesNotThrow(() -> registerService.register(body));
            verify(userService).registerUser(any(SysUser.class));
        }
    }

    private RegisterBody validRegisterBody()
    {
        RegisterBody body = new RegisterBody();
        body.setUsername("newuser");
        body.setPassword("password123");
        body.setPhoneNumber("13800138000");
        body.setNickName("New User");
        return body;
    }
}
