package com.cat2bug.framework.security.filter;

import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.framework.web.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Git Basic 必须走「不依赖 AuthenticationContextHolder」的加载路径，否则永远 401。
 */
@ExtendWith(MockitoExtension.class)
class GitUserDetailsServiceTest
{
    @Mock
    private UserDetailsServiceImpl delegate;

    @InjectMocks
    private GitUserDetailsService service;

    @Test
    void loadUserByUsername_usesWithoutPasswordCheck()
    {
        LoginUser loginUser = new LoginUser();
        when(delegate.loadUserByUsernameWithoutPasswordCheck("admin")).thenReturn(loginUser);

        UserDetails result = service.loadUserByUsername("admin");

        assertSame(loginUser, result);
        verify(delegate).loadUserByUsernameWithoutPasswordCheck("admin");
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void loadUserByUsername_foldsRuntimeExceptionToUsernameNotFound()
    {
        when(delegate.loadUserByUsernameWithoutPasswordCheck("gone"))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("gone"));
    }
}
