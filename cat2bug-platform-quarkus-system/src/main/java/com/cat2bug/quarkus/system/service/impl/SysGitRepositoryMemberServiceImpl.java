package com.cat2bug.quarkus.system.service.impl;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.cat2bug.quarkus.common.constant.HttpStatus;
import com.cat2bug.quarkus.common.domain.SysUser;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.DateUtils;
import com.cat2bug.quarkus.common.util.MessageUtils;
import com.cat2bug.quarkus.framework.security.SecurityUtils;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.common.domain.SysGitRepository;
import com.cat2bug.quarkus.common.domain.SysGitRepositoryMember;
import com.cat2bug.quarkus.common.domain.git.GitRepoMembersFromRepoResult;
import com.cat2bug.quarkus.common.domain.git.GitRepoMyAccess;
import com.cat2bug.quarkus.common.domain.git.GitRepositoryAclRoles;
import com.cat2bug.quarkus.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.quarkus.system.mapper.SysGitRepositoryMemberMapper;
import com.cat2bug.quarkus.system.mapper.SysUserMapper;
import com.cat2bug.quarkus.system.service.ISysGitRepositoryMemberService;
import com.cat2bug.quarkus.system.service.git.GitRepositoryAccessService;
import jakarta.transaction.Transactional;

/**
 * 仓库成员 ACL：CRUD + 最后一位 admin 保护 + 创建者种子。
 */
@ApplicationScoped
public class SysGitRepositoryMemberServiceImpl implements ISysGitRepositoryMemberService
{
    @Inject
    private SysGitRepositoryMemberMapper memberMapper;

    @Inject
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Inject
    private SysUserMapper sysUserMapper;

    @Inject
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Override
    public List<SysGitRepositoryMember> selectMembersByRepositoryId(Long repositoryId)
    {
        requireRepository(repositoryId);
        assertManage(repositoryId);
        return memberMapper.selectMembersByRepositoryId(repositoryId);
    }

    @Override
    public SysGitRepositoryMember selectByRepositoryIdAndUserId(Long repositoryId, Long userId)
    {
        return memberMapper.selectByRepositoryIdAndUserId(repositoryId, userId);
    }

    @Override
    @Transactional
    public int insertMember(Long repositoryId, Long userId, String role)
    {
        requireRepository(repositoryId);
        assertManage(repositoryId);
        return doInsertMember(repositoryId, userId, role);
    }

    @Override
    @Transactional
    public int insertMemberFromInvite(Long repositoryId, Long userId, String role)
    {
        requireRepository(repositoryId);
        return doInsertMember(repositoryId, userId, role);
    }

    private int doInsertMember(Long repositoryId, Long userId, String role)
    {
        if (userId == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.user-required"));
        }
        String normalized = normalizeRole(role);
        SysUser user = sysUserMapper.selectUserById(userId);
        if (user == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.user-not-found"));
        }
        if (memberMapper.selectByRepositoryIdAndUserId(repositoryId, userId) != null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.duplicate"));
        }

        SysGitRepositoryMember row = new SysGitRepositoryMember();
        row.setRepositoryId(repositoryId);
        row.setUserId(userId);
        row.setRole(normalized);
        String operator = SecurityUtils.getUsername();
        if (StringUtils.isEmpty(operator))
        {
            operator = "system";
        }
        row.setCreateBy(operator);
        row.setUpdateBy(operator);
        row.setCreateTime(DateUtils.getNowDate());
        row.setUpdateTime(DateUtils.getNowDate());
        return memberMapper.insertSysGitRepositoryMember(row);
    }

    @Override
    @Transactional
    public int updateMemberRole(Long repositoryId, Long userId, String role)
    {
        requireRepository(repositoryId);
        assertManage(repositoryId);
        if (userId == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.user-required"));
        }
        String normalized = normalizeRole(role);
        SysGitRepositoryMember existing = memberMapper.selectByRepositoryIdAndUserId(repositoryId, userId);
        if (existing == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.not-found"));
        }
        if (GitRepositoryAclRoles.ADMIN.equals(existing.getRole()) && !GitRepositoryAclRoles.ADMIN.equals(normalized))
        {
            rejectIfLastAdmin(repositoryId);
        }

        existing.setRole(normalized);
        existing.setUpdateBy(SecurityUtils.getUsername());
        existing.setUpdateTime(DateUtils.getNowDate());
        return memberMapper.updateMemberRole(existing);
    }

    @Override
    @Transactional
    public int deleteMember(Long repositoryId, Long userId)
    {
        requireRepository(repositoryId);
        assertManage(repositoryId);
        if (userId == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.user-required"));
        }
        SysGitRepositoryMember existing = memberMapper.selectByRepositoryIdAndUserId(repositoryId, userId);
        if (existing == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.not-found"));
        }
        if (GitRepositoryAclRoles.ADMIN.equals(existing.getRole()))
        {
            rejectIfLastAdmin(repositoryId);
        }
        return memberMapper.deleteByRepositoryIdAndUserId(repositoryId, userId);
    }

    @Override
    @Transactional
    public void ensureCreatorAdmin(Long repositoryId, Long userId)
    {
        if (repositoryId == null || userId == null)
        {
            return;
        }
        if (memberMapper.selectByRepositoryIdAndUserId(repositoryId, userId) != null)
        {
            return;
        }
        SysGitRepositoryMember row = new SysGitRepositoryMember();
        row.setRepositoryId(repositoryId);
        row.setUserId(userId);
        row.setRole(GitRepositoryAclRoles.ADMIN);
        String operator = SecurityUtils.getUsername();
        if (StringUtils.isEmpty(operator))
        {
            operator = "system";
        }
        row.setCreateBy(operator);
        row.setUpdateBy(operator);
        row.setCreateTime(DateUtils.getNowDate());
        row.setUpdateTime(DateUtils.getNowDate());
        memberMapper.insertSysGitRepositoryMember(row);
    }

    @Override
    public GitRepoMyAccess getMyAccess(Long repositoryId)
    {
        requireRepository(repositoryId);
        Long userId = SecurityUtils.getUserId();
        boolean canPull = gitRepositoryAccessService.canPull(userId, repositoryId);
        boolean canPush = gitRepositoryAccessService.canPush(userId, repositoryId);
        boolean canManage = gitRepositoryAccessService.canManage(userId, repositoryId);
        String role = gitRepositoryAccessService.resolveEffectiveRole(userId, repositoryId);
        return new GitRepoMyAccess(role, canPull, canPush, canManage);
    }

    @Override
    public List<SysGitRepository> listInviteSources(Long repositoryId)
    {
        SysGitRepository target = requireRepository(repositoryId);
        assertManage(repositoryId);
        Long userId = SecurityUtils.getUserId();
        SysGitRepository query = new SysGitRepository();
        query.setProjectId(target.getProjectId());
        List<SysGitRepository> inProject = sysGitRepositoryMapper.selectSysGitRepositoryList(query);
        if (inProject == null || inProject.isEmpty())
        {
            return Collections.emptyList();
        }
        return inProject.stream()
                .filter(repo -> repo != null && repo.getRepositoryId() != null)
                .filter(repo -> !Objects.equals(repositoryId, repo.getRepositoryId()))
                .filter(repo -> gitRepositoryAccessService.canManage(userId, repo.getRepositoryId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SysGitRepositoryMember> listInviteSourceMembers(Long repositoryId, Long sourceRepositoryId)
    {
        assertManage(repositoryId);
        requireEligibleInviteSource(repositoryId, sourceRepositoryId);
        return memberMapper.selectMembersByRepositoryId(sourceRepositoryId);
    }

    @Override
    @Transactional
    public GitRepoMembersFromRepoResult insertMembersFromRepo(Long repositoryId, Long sourceRepositoryId,
            List<Long> userIds, String role)
    {
        requireRepository(repositoryId);
        assertManage(repositoryId);
        requireEligibleInviteSource(repositoryId, sourceRepositoryId);
        if (userIds == null || userIds.isEmpty())
        {
            throw new ApiException(MessageUtils.message("git.repo.member.from-repo.users-required"));
        }
        String normalized = normalizeRole(role);
        int added = 0;
        int skipped = 0;
        // 去重，保持顺序
        List<Long> distinctIds = new ArrayList<>();
        for (Long userId : userIds)
        {
            if (userId != null && !distinctIds.contains(userId))
            {
                distinctIds.add(userId);
            }
        }
        for (Long userId : distinctIds)
        {
            if (memberMapper.selectByRepositoryIdAndUserId(repositoryId, userId) != null)
            {
                skipped++;
                continue;
            }
            doInsertMember(repositoryId, userId, normalized);
            added++;
        }
        return new GitRepoMembersFromRepoResult(added, skipped);
    }

    /**
     * 源仓须存在、非本仓、同项目，且调用方对源仓 effective manage。
     */
    private void requireEligibleInviteSource(Long targetRepositoryId, Long sourceRepositoryId)
    {
        if (sourceRepositoryId == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.from-repo.source-invalid"));
        }
        if (Objects.equals(targetRepositoryId, sourceRepositoryId))
        {
            throw new ApiException(MessageUtils.message("git.repo.member.from-repo.source-invalid"));
        }
        SysGitRepository target = requireRepository(targetRepositoryId);
        SysGitRepository source = requireRepository(sourceRepositoryId);
        if (!Objects.equals(target.getProjectId(), source.getProjectId()))
        {
            throw new ApiException(MessageUtils.message("git.repo.member.from-repo.source-invalid"));
        }
        if (!gitRepositoryAccessService.canManage(SecurityUtils.getUserId(), sourceRepositoryId))
        {
            throw new ApiException(MessageUtils.message("git.repo.member.from-repo.source-invalid"),
                    HttpStatus.FORBIDDEN);
        }
    }

    private void rejectIfLastAdmin(Long repositoryId)
    {
        if (memberMapper.countAdminsByRepositoryId(repositoryId) <= 1)
        {
            throw new ApiException(MessageUtils.message("git.repo.member.last-admin"));
        }
    }

    private String normalizeRole(String role)
    {
        if (StringUtils.isEmpty(role) || !GitRepositoryAclRoles.ALL.contains(role.trim()))
        {
            throw new ApiException(MessageUtils.message("git.repo.member.role-invalid"));
        }
        return role.trim();
    }

    private SysGitRepository requireRepository(Long repositoryId)
    {
        if (repositoryId == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.repository-id-required"));
        }
        SysGitRepository repo = sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.not-found"), HttpStatus.NOT_FOUND);
        }
        return repo;
    }

    private void assertManage(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canManage(SecurityUtils.getUserId(), repositoryId))
        {
            throw new ApiException(MessageUtils.message("no-permission"), HttpStatus.FORBIDDEN);
        }
    }
}
