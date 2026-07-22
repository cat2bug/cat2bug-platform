package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryMember;
import com.cat2bug.system.domain.git.GitRepoMembersFromRepoResult;
import com.cat2bug.system.domain.git.GitRepositoryAclRoles;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMemberMapper;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成员 ACL CRUD：邀请、重复拒绝、最后一位 admin 保护。
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SysGitRepositoryMemberServiceImplTest
{
    private static final Long REPOSITORY_ID = 100L;
    private static final Long SOURCE_REPOSITORY_ID = 200L;
    private static final Long OTHER_PROJECT_REPO_ID = 300L;
    private static final Long PROJECT_ID = 10L;
    private static final Long OTHER_PROJECT_ID = 99L;
    private static final Long MANAGER_ID = 1L;
    private static final Long TARGET_USER_ID = 50L;
    private static final Long EXISTING_USER_ID = 51L;

    @Mock
    private SysGitRepositoryMemberMapper memberMapper;

    @Mock
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private GitRepositoryAccessService gitRepositoryAccessService;

    @InjectMocks
    private SysGitRepositoryMemberServiceImpl service;

    private Runnable messageCleanup;

    @Before
    public void setUp()
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        SysGitRepository repo = new SysGitRepository();
        repo.setRepositoryId(REPOSITORY_ID);
        repo.setProjectId(PROJECT_ID);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(REPOSITORY_ID)).thenReturn(repo);

        SysGitRepository source = new SysGitRepository();
        source.setRepositoryId(SOURCE_REPOSITORY_ID);
        source.setProjectId(PROJECT_ID);
        source.setName("Source Repo");
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(SOURCE_REPOSITORY_ID)).thenReturn(source);

        SysGitRepository otherProject = new SysGitRepository();
        otherProject.setRepositoryId(OTHER_PROJECT_REPO_ID);
        otherProject.setProjectId(OTHER_PROJECT_ID);
        when(sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(OTHER_PROJECT_REPO_ID)).thenReturn(otherProject);

        when(gitRepositoryAccessService.canManage(MANAGER_ID, REPOSITORY_ID)).thenReturn(true);
        when(gitRepositoryAccessService.canManage(MANAGER_ID, SOURCE_REPOSITORY_ID)).thenReturn(true);
        loginAs(MANAGER_ID);
    }

    @After
    public void tearDown()
    {
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    public void insertMember_inviteNonProjectUser_succeeds()
    {
        SysUser user = new SysUser();
        user.setUserId(TARGET_USER_ID);
        when(sysUserMapper.selectUserById(null, null, TARGET_USER_ID)).thenReturn(user);
        when(memberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, TARGET_USER_ID)).thenReturn(null);
        when(memberMapper.insertSysGitRepositoryMember(any())).thenReturn(1);

        int rows = service.insertMember(REPOSITORY_ID, TARGET_USER_ID, GitRepositoryAclRoles.WRITE);

        assertEquals(1, rows);
        verify(memberMapper).insertSysGitRepositoryMember(any());
    }

    @Test
    public void insertMember_duplicate_rejected()
    {
        SysUser user = new SysUser();
        user.setUserId(TARGET_USER_ID);
        when(sysUserMapper.selectUserById(null, null, TARGET_USER_ID)).thenReturn(user);
        SysGitRepositoryMember existing = new SysGitRepositoryMember();
        existing.setRole(GitRepositoryAclRoles.READ);
        when(memberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, TARGET_USER_ID)).thenReturn(existing);

        try
        {
            service.insertMember(REPOSITORY_ID, TARGET_USER_ID, GitRepositoryAclRoles.WRITE);
            fail("expected duplicate reject");
        }
        catch (ServiceException expected)
        {
            // duplicate
        }
        verify(memberMapper, never()).insertSysGitRepositoryMember(any());
    }

    @Test
    public void deleteMember_lastAdmin_rejected()
    {
        SysGitRepositoryMember existing = new SysGitRepositoryMember();
        existing.setUserId(TARGET_USER_ID);
        existing.setRole(GitRepositoryAclRoles.ADMIN);
        when(memberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, TARGET_USER_ID)).thenReturn(existing);
        when(memberMapper.countAdminsByRepositoryId(REPOSITORY_ID)).thenReturn(1);

        try
        {
            service.deleteMember(REPOSITORY_ID, TARGET_USER_ID);
            fail("expected last-admin reject");
        }
        catch (ServiceException expected)
        {
            // last admin
        }
        verify(memberMapper, never()).deleteByRepositoryIdAndUserId(any(), any());
    }

    @Test
    public void updateMemberRole_demoteLastAdmin_rejected()
    {
        SysGitRepositoryMember existing = new SysGitRepositoryMember();
        existing.setUserId(TARGET_USER_ID);
        existing.setRole(GitRepositoryAclRoles.ADMIN);
        when(memberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, TARGET_USER_ID)).thenReturn(existing);
        when(memberMapper.countAdminsByRepositoryId(REPOSITORY_ID)).thenReturn(1);

        try
        {
            service.updateMemberRole(REPOSITORY_ID, TARGET_USER_ID, GitRepositoryAclRoles.WRITE);
            fail("expected last-admin demote reject");
        }
        catch (ServiceException expected)
        {
            // last admin demote
        }
        verify(memberMapper, never()).updateMemberRole(any());
    }

    @Test
    public void deleteMember_secondAdmin_allowed()
    {
        SysGitRepositoryMember existing = new SysGitRepositoryMember();
        existing.setUserId(TARGET_USER_ID);
        existing.setRole(GitRepositoryAclRoles.ADMIN);
        when(memberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, TARGET_USER_ID)).thenReturn(existing);
        when(memberMapper.countAdminsByRepositoryId(REPOSITORY_ID)).thenReturn(2);
        when(memberMapper.deleteByRepositoryIdAndUserId(REPOSITORY_ID, TARGET_USER_ID)).thenReturn(1);

        assertEquals(1, service.deleteMember(REPOSITORY_ID, TARGET_USER_ID));
        verify(memberMapper).deleteByRepositoryIdAndUserId(eq(REPOSITORY_ID), eq(TARGET_USER_ID));
    }

    @Test
    public void listInviteSources_excludesSelfAndNonManageable()
    {
        SysGitRepository self = new SysGitRepository();
        self.setRepositoryId(REPOSITORY_ID);
        self.setProjectId(PROJECT_ID);
        SysGitRepository manageable = new SysGitRepository();
        manageable.setRepositoryId(SOURCE_REPOSITORY_ID);
        manageable.setProjectId(PROJECT_ID);
        SysGitRepository noManage = new SysGitRepository();
        noManage.setRepositoryId(400L);
        noManage.setProjectId(PROJECT_ID);
        when(sysGitRepositoryMapper.selectSysGitRepositoryList(any())).thenReturn(
                Arrays.asList(self, manageable, noManage));
        when(gitRepositoryAccessService.canManage(MANAGER_ID, 400L)).thenReturn(false);

        List<SysGitRepository> sources = service.listInviteSources(REPOSITORY_ID);

        assertEquals(1, sources.size());
        assertEquals(SOURCE_REPOSITORY_ID, sources.get(0).getRepositoryId());
    }

    @Test
    public void fromRepo_rejectsWhenSourceNotSameProject()
    {
        try
        {
            service.insertMembersFromRepo(REPOSITORY_ID, OTHER_PROJECT_REPO_ID,
                    Collections.singletonList(TARGET_USER_ID), GitRepositoryAclRoles.WRITE);
            fail("expected cross-project reject");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("from-repo.source-invalid"));
        }
        verify(memberMapper, never()).insertSysGitRepositoryMember(any());
    }

    @Test
    public void fromRepo_rejectsWhenCallerCannotManageSource()
    {
        when(gitRepositoryAccessService.canManage(MANAGER_ID, SOURCE_REPOSITORY_ID)).thenReturn(false);
        try
        {
            service.insertMembersFromRepo(REPOSITORY_ID, SOURCE_REPOSITORY_ID,
                    Collections.singletonList(TARGET_USER_ID), GitRepositoryAclRoles.WRITE);
            fail("expected source manage reject");
        }
        catch (ServiceException expected)
        {
            // ineligible source
        }
        verify(memberMapper, never()).insertSysGitRepositoryMember(any());
    }

    @Test
    public void fromRepo_rejectsWhenSourceIsSelf()
    {
        try
        {
            service.insertMembersFromRepo(REPOSITORY_ID, REPOSITORY_ID,
                    Collections.singletonList(TARGET_USER_ID), GitRepositoryAclRoles.WRITE);
            fail("expected self-source reject");
        }
        catch (ServiceException expected)
        {
            // self not allowed
        }
        verify(memberMapper, never()).insertSysGitRepositoryMember(any());
    }

    @Test
    public void fromRepo_skipsExistingMembersAndInsertsOthers()
    {
        SysUser newUser = new SysUser();
        newUser.setUserId(TARGET_USER_ID);
        when(sysUserMapper.selectUserById(null, null, TARGET_USER_ID)).thenReturn(newUser);
        when(memberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, EXISTING_USER_ID))
                .thenReturn(new SysGitRepositoryMember());
        when(memberMapper.selectByRepositoryIdAndUserId(REPOSITORY_ID, TARGET_USER_ID)).thenReturn(null);
        when(memberMapper.insertSysGitRepositoryMember(any())).thenReturn(1);

        GitRepoMembersFromRepoResult result = service.insertMembersFromRepo(
                REPOSITORY_ID, SOURCE_REPOSITORY_ID,
                Arrays.asList(EXISTING_USER_ID, TARGET_USER_ID),
                GitRepositoryAclRoles.WRITE);

        assertEquals(1, result.getAdded());
        assertEquals(1, result.getSkipped());
        verify(memberMapper, times(1)).insertSysGitRepositoryMember(any());
    }

    private void loginAs(Long userId)
    {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setUserName("manager");
        LoginUser loginUser = new LoginUser(userId, null, sysUser, java.util.Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, java.util.Collections.emptyList()));
    }
}
