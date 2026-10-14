package com.cat2bug.quarkus.system.service.git;

import com.cat2bug.quarkus.common.domain.git.GitRepositoryRef;

import java.util.Optional;

/**
 * 按项目ID + slug 定位 Git 仓库磁盘引用。
 *
 * <p>Smart HTTP（openspec/changes/project-git-repositories 第3节）用它把 clone URL 中的
 * {@code {projectId}/{slug}.git} 解析成磁盘上的 bare 仓路径。仓库 CRUD（创建/改名/删除）
 * 由第2节的 {@code ISysGitRepositoryService}/{@code SysGitRepositoryMapper} 负责；本接口是
 * 面向 Smart HTTP 的窄读接口，实现委托 {@code SysGitRepositoryMapper} 的原始查询（不复用
 * {@code ISysGitRepositoryService}，因为后者按当前登录用户 SecurityContext 做的是 REST 浏览
 * 语义的 pull 权限校验，而 Smart HTTP 需要按 pull/push/manage 分别判定）。</p>
 */
public interface GitRepositoryLookup
{
    /**
     * 查询仓库引用。
     *
     * @param projectId 项目ID
     * @param slug      仓库 slug
     * @return 存在则返回，否则 {@link Optional#empty()}
     */
    Optional<GitRepositoryRef> findByProjectIdAndSlug(Long projectId, String slug);

    /**
     * 按团队/项目 path key + slug 查询仓库引用（namespaced Smart HTTP）。
     */
    Optional<GitRepositoryRef> findByTeamKeyAndProjectKeyAndSlug(String teamKey, String projectKey, String slug);
}
