package com.cat2bug.system.service.impl;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysProject;
import com.cat2bug.system.domain.SysUserProject;
import com.cat2bug.system.mapper.SysGitRepositoryMemberMapper;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SysProjectPathResolveServiceTest
{
    private static final Long USER_ID = 7L;
    private static final Long PROJECT_ID = 42L;

    @Mock
    private SysProjectMapper sysProjectMapper;
    @Mock
    private SysUserProjectMapper sysUserProjectMapper;
    @Mock
    private SysGitRepositoryMemberMapper sysGitRepositoryMemberMapper;

    @InjectMocks
    private SysProjectServiceImpl sysProjectService;

    private Runnable messageCleanup;

    @Before
    public void setUp()
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        LoginUser loginUser = new LoginUser();
        SysUser user = new SysUser();
        user.setUserId(USER_ID);
        loginUser.setUser(user);
        loginUser.setUserId(USER_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    @After
    public void tearDown()
    {
        SecurityContextHolder.clearContext();
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
    }

    @Test
    public void resolveByPath_returnsProjectWhenMember()
    {
        SysProject byKey = projectByKey();
        SysProject enriched = projectByKey();
        enriched.setCollect(true);
        when(sysProjectMapper.selectSysProjectByTeamAndProjectKey("acme", "platform")).thenReturn(byKey);
        when(sysUserProjectMapper.selectSysUserProjectList(any(SysUserProject.class)))
                .thenReturn(Collections.singletonList(new SysUserProject()));
        when(sysProjectMapper.selectSysProjectByProjectId(eq(PROJECT_ID), eq(USER_ID))).thenReturn(enriched);

        SysProject result = sysProjectService.selectSysProjectByTeamAndProjectKey("acme", "platform");

        assertNotNull(result);
        assertEquals(PROJECT_ID, result.getProjectId());
        assertEquals("acme", result.getTeamKey());
        assertEquals("platform", result.getProjectKey());
    }

    @Test
    public void resolveByPath_notFoundWhenKeysUnknown()
    {
        when(sysProjectMapper.selectSysProjectByTeamAndProjectKey("missing", "nope")).thenReturn(null);
        try
        {
            sysProjectService.selectSysProjectByTeamAndProjectKey("missing", "nope");
            fail("expected ServiceException");
        }
        catch (ServiceException e)
        {
            assertEquals(Integer.valueOf(HttpStatus.NOT_FOUND), e.getCode());
        }
    }

    @Test
    public void resolveByPath_forbiddenWhenNotMember()
    {
        SysProject byKey = projectByKey();
        when(sysProjectMapper.selectSysProjectByTeamAndProjectKey("acme", "platform")).thenReturn(byKey);
        when(sysUserProjectMapper.selectSysUserProjectList(any(SysUserProject.class))).thenReturn(Collections.emptyList());
        when(sysGitRepositoryMemberMapper.countByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(0);
        try
        {
            sysProjectService.selectSysProjectByTeamAndProjectKey("acme", "platform");
            fail("expected ServiceException");
        }
        catch (ServiceException e)
        {
            assertEquals(Integer.valueOf(HttpStatus.FORBIDDEN), e.getCode());
        }
    }

    @Test
    public void resolveByPath_allowsWhenRepoAclMemberOnly()
    {
        SysProject byKey = projectByKey();
        SysProject enriched = projectByKey();
        when(sysProjectMapper.selectSysProjectByTeamAndProjectKey("acme", "platform")).thenReturn(byKey);
        when(sysUserProjectMapper.selectSysUserProjectList(any(SysUserProject.class))).thenReturn(Collections.emptyList());
        when(sysGitRepositoryMemberMapper.countByProjectIdAndUserId(PROJECT_ID, USER_ID)).thenReturn(1);
        when(sysProjectMapper.selectSysProjectByProjectId(eq(PROJECT_ID), eq(USER_ID))).thenReturn(enriched);

        SysProject result = sysProjectService.selectSysProjectByTeamAndProjectKey("acme", "platform");

        assertNotNull(result);
        assertEquals(PROJECT_ID, result.getProjectId());
    }

    private SysProject projectByKey()
    {
        SysProject p = new SysProject();
        p.setProjectId(PROJECT_ID);
        p.setTeamId(3L);
        p.setTeamKey("acme");
        p.setProjectKey("platform");
        p.setProjectName("Platform");
        p.setCreateBy("99");
        return p;
    }
}
