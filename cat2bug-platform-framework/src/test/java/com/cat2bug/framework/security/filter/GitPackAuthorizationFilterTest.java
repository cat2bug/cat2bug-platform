package com.cat2bug.framework.security.filter;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.framework.security.git.GitPackRequestContext;
import com.cat2bug.system.domain.git.GitRepositoryRef;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import com.cat2bug.system.service.git.GitRepositoryLookup;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * specs/git-smart-http/spec.md 授权部分（3.3/3.4）在过滤器层的行为验证。
 */
@ExtendWith(MockitoExtension.class)
class GitPackAuthorizationFilterTest
{
    private static final Long PROJECT_ID = 10L;
    private static final Long REPOSITORY_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final String SLUG = "backend";

    @Mock
    private GitRepositoryLookup gitRepositoryLookup;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private GitPackAuthorizationFilter filter;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp()
    {
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void nonMatchingPath_returns404_andNeverCallsChain() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/git/p/" + PROJECT_ID + "/" + SLUG + ".git/HEAD");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(404, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void infoRefs_withoutServiceParam_returns404()throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/git/p/" + PROJECT_ID + "/" + SLUG + ".git/info/refs");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(404, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void unauthenticated_delegatesToChain_withoutOwnSendError() throws Exception
    {
        MockHttpServletRequest request = infoRefsRequest("git-upload-pack");
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        filter.doFilterInternal(request, response, filterChain);

        // 交给下游 AuthorizationFilter + BasicAuthenticationEntryPoint 走标准 401 挑战，本过滤器不抢答
        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        verify(gitRepositoryLookup, never()).findByProjectIdAndSlug(anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void repositoryNotFound_returns404() throws Exception
    {
        authenticateAs(USER_ID);
        MockHttpServletRequest request = infoRefsRequest("git-upload-pack");
        when(gitRepositoryLookup.findByProjectIdAndSlug(PROJECT_ID, SLUG)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(404, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void pull_withoutPullRole_returns403() throws Exception
    {
        authenticateAs(USER_ID);
        MockHttpServletRequest request = infoRefsRequest("git-upload-pack");
        GitRepositoryRef ref = new GitRepositoryRef(1L, PROJECT_ID, SLUG, "/tmp/repo.git", "main");
        when(gitRepositoryLookup.findByProjectIdAndSlug(PROJECT_ID, SLUG)).thenReturn(Optional.of(ref));
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void push_withPushRole_grantsAccess_andAttachesContext() throws Exception
    {
        authenticateAs(USER_ID);
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/git/p/" + PROJECT_ID + "/" + SLUG + ".git/git-receive-pack");
        GitRepositoryRef ref = new GitRepositoryRef(1L, PROJECT_ID, SLUG, "/tmp/repo.git", "main");
        when(gitRepositoryLookup.findByProjectIdAndSlug(PROJECT_ID, SLUG)).thenReturn(Optional.of(ref));
        when(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID)).thenReturn(true);
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        GitPackRequestContext context = (GitPackRequestContext) request.getAttribute(GitPackRequestContext.REQUEST_ATTRIBUTE);
        assertSame(ref, context.getRepository());
        assertEquals(false, context.isForcePushAllowed());
    }

    @Test
    void push_withManageRole_marksForcePushAllowed() throws Exception
    {
        authenticateAs(USER_ID);
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/git/p/" + PROJECT_ID + "/" + SLUG + ".git/git-receive-pack");
        GitRepositoryRef ref = new GitRepositoryRef(1L, PROJECT_ID, SLUG, "/tmp/repo.git", "main");
        when(gitRepositoryLookup.findByProjectIdAndSlug(PROJECT_ID, SLUG)).thenReturn(Optional.of(ref));
        when(gitRepositoryAccessService.canPush(USER_ID, REPOSITORY_ID)).thenReturn(true);
        when(gitRepositoryAccessService.canManage(USER_ID, REPOSITORY_ID)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        GitPackRequestContext context = (GitPackRequestContext) request.getAttribute(GitPackRequestContext.REQUEST_ATTRIBUTE);
        assertEquals(true, context.isForcePushAllowed());
    }

    @Test
    void namespacedPull_withPullRole_grantsAccess() throws Exception
    {
        authenticateAs(USER_ID);
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/acme/demo/code/" + SLUG + ".git/info/refs");
        request.setParameter("service", "git-upload-pack");
        GitRepositoryRef ref = new GitRepositoryRef(1L, PROJECT_ID, SLUG, "/tmp/repo.git", "main");
        when(gitRepositoryLookup.findByTeamKeyAndProjectKeyAndSlug("acme", "demo", SLUG))
                .thenReturn(Optional.of(ref));
        when(gitRepositoryAccessService.canPull(USER_ID, REPOSITORY_ID)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(gitRepositoryLookup, never()).findByProjectIdAndSlug(anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void namespacedPath_unknownRepo_returns404() throws Exception
    {
        authenticateAs(USER_ID);
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/acme/demo/code/" + SLUG + ".git/info/refs");
        request.setParameter("service", "git-upload-pack");
        when(gitRepositoryLookup.findByTeamKeyAndProjectKeyAndSlug("acme", "demo", SLUG))
                .thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(404, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    private MockHttpServletRequest infoRefsRequest(String service)
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/git/p/" + PROJECT_ID + "/" + SLUG + ".git/info/refs");
        request.setParameter("service", service);
        return request;
    }

    private void authenticateAs(Long userId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setUserName("develop");
        sysUser.setPassword("$2a$hash");
        LoginUser loginUser = new LoginUser(userId, null, sysUser, java.util.Collections.emptySet());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
