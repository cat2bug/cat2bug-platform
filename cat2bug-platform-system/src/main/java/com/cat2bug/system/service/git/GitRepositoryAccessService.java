package com.cat2bug.system.service.git;

/**
 * Git 仓库角色矩阵判定（design.md D3 / specs/git-smart-http/spec.md）。
 *
 * <p>角色取「用户在目标项目下的项目角色」之并集，<b>不</b>因系统超管或团队管理员身份而绕过——
 * 必须显式持有 {@code project.create-by}/{@code project.admin}/{@code project.develop}/
 * {@code project.tester} 之一才允许 pull/push；force-push 仅
 * {@code project.create-by}/{@code project.admin}。</p>
 */
public interface GitRepositoryAccessService
{
    /**
     * 是否可 pull（upload-pack）。
     */
    boolean canPull(Long userId, Long projectId);

    /**
     * 是否可 push（receive-pack，快进更新）。
     */
    boolean canPush(Long userId, Long projectId);

    /**
     * 是否可 force-push（非快进更新）/ 管理仓库。
     */
    boolean canManage(Long userId, Long projectId);
}
