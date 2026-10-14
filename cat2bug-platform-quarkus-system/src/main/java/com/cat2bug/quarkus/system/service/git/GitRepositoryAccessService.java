package com.cat2bug.quarkus.system.service.git;

/**
 * Git 仓库有效权限判定（仓级 ACL 优先，否则回落项目角色）。
 *
 * <p>签名以 {@code repositoryId} 为第二参数：有 ACL 行时仅该角色生效；无行时加载仓库的
 * {@code projectId} 再按项目角色矩阵（develop/tester → pull+push；create-by/admin → manage）。
 * 系统超级管理员（与菜单权限 {@code *:*:*} 同口径）拥有全部仓库的完整权限；团队管理员不绕过。</p>
 */
public interface GitRepositoryAccessService
{
    /**
     * 是否可 pull（upload-pack / 浏览只读）。
     */
    boolean canPull(Long userId, Long repositoryId);

    /**
     * 是否可 push（receive-pack，快进更新）。
     */
    boolean canPush(Long userId, Long repositoryId);

    /**
     * 是否可 force-push / 管理仓库 / 管理 ACL。
     */
    boolean canManage(Long userId, Long repositoryId);

    /**
     * 有效仓角色：{@code read}/{@code write}/{@code admin}；无权限时返回 {@code null}。
     * <p>有 ACL 行返回该行角色；否则项目 manage → {@code admin}，项目 pull/push → {@code write}。</p>
     */
    String resolveEffectiveRole(Long userId, Long repositoryId);

    /**
     * 是否系统超级管理员（userId=1 或拥有系统角色 {@code admin}）。
     */
    boolean isSystemAdmin(Long userId);
}
