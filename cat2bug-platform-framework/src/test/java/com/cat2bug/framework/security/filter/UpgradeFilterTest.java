package com.cat2bug.framework.security.filter;

import com.cat2bug.common.config.UpgradeSupport;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpgradeFilterTest
{
    @Mock
    private UpgradeService upgradeService;

    @Mock
    private InstallService installService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private UpgradeFilter upgradeFilter;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp()
    {
        response = new MockHttpServletResponse();
        lenient().when(installService.isInstalled()).thenReturn(true);
    }

    @Test
    void doFilterInternal_passesThroughForFreshInstallSetup() throws Exception
    {
        when(installService.isInstalled()).thenReturn(false);
        when(upgradeService.isUpgradeRequired()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/setup/submit");

        upgradeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_passesThroughWhenUpgradeNotActive() throws Exception
    {
        when(upgradeService.isUpgradeActive()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");

        upgradeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_blocksLoginWhenUpgradePending() throws Exception
    {
        when(upgradeService.isUpgradeActive()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");

        upgradeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertTrue(response.getContentAsString().contains("升级"));
    }

    @Test
    void doFilterInternal_blocksLoginWhenUpgradeRequiredButNotYetActive() throws Exception
    {
        when(upgradeService.isUpgradeActive()).thenReturn(false);
        when(upgradeService.isUpgradeRequired()).thenReturn(true);
        when(upgradeService.resolveState()).thenReturn(UpgradeSupport.STATE_PENDING);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");

        upgradeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_allowsUpgradeStatusWhenActive() throws Exception
    {
        when(upgradeService.isUpgradeActive()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/upgrade/status");

        upgradeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void isAllowedDuringUpgrade_allowsVersionAndStaticAssets()
    {
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/version", "GET"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/static/js/app.js", "GET"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/setup/test/path", "POST"));
        assertFalse(UpgradeFilter.isAllowedDuringUpgrade("/system/user/list", "GET"));
    }
}
