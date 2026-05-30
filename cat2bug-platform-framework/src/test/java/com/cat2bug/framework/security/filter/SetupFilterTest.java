package com.cat2bug.framework.security.filter;

import com.cat2bug.framework.service.InstallService;
import com.cat2bug.framework.service.UpgradeService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetupFilterTest
{
    @Mock
    private InstallService installService;

    @Mock
    private UpgradeService upgradeService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private SetupFilter setupFilter;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp()
    {
        response = new MockHttpServletResponse();
        lenient().when(installService.isInstalled()).thenReturn(true);
        lenient().when(installService.needsRestart()).thenReturn(false);
    }

    @Test
    void doFilterInternal_allowsSetupTestDuringUpgrade() throws Exception
    {
        when(upgradeService.isUpgradeRequired()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/setup/test/database");

        setupFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_blocksSetupSubmitAfterInstall() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/setup/submit");

        setupFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_allowsSetupSubmitWhileRestartPending() throws Exception
    {
        when(installService.needsRestart()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/setup/submit");

        setupFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }
}
