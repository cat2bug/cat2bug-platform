package com.cat2bug.system.service.impl.git;

import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.git.GitRepositoryRef;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.git.GitPathValidator;
import com.cat2bug.system.service.git.GitRepositoryLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 委托正式 CRUD Mapper {@link SysGitRepositoryMapper}（已落地，见 tasks 2.1）做原始查询，
 * 不重复实现一套 SQL；Smart HTTP / 框架层只依赖 {@link GitRepositoryLookup} 这个窄接口与
 * {@link GitRepositoryRef} 这个最小 DTO，不直接耦合完整的 {@link SysGitRepository} 领域对象。
 *
 * <p>刻意不调用 {@code ISysGitRepositoryService}（该 Service 内部按「当前登录用户」的
 * SecurityContext 做 pull 权限校验，语义是「REST 浏览」）：Smart HTTP 需要按 pull/push/manage
 * 三种不同操作分别判定（见 {@code GitRepositoryAccessService}），且请求发起者是 HTTP Basic
 * 认证后的用户而非 REST 会话，用原始 Mapper 更贴合。</p>
 */
@Service
public class GitRepositoryLookupImpl implements GitRepositoryLookup
{
    @Autowired
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Autowired
    private GitPathValidator gitPathValidator;

    @Override
    public Optional<GitRepositoryRef> findByProjectIdAndSlug(Long projectId, String slug)
    {
        if (projectId == null || slug == null || slug.isEmpty())
        {
            return Optional.empty();
        }
        SysGitRepository repository = sysGitRepositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(projectId, slug);
        if (repository == null)
        {
            return Optional.empty();
        }
        String diskPath = gitPathValidator.resolveStoredDiskPath(repository.getDiskPath());
        return Optional.of(new GitRepositoryRef(repository.getRepositoryId(), repository.getProjectId(),
                repository.getSlug(), diskPath, repository.getDefaultBranch()));
    }
}
