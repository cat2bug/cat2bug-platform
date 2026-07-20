package com.cat2bug.framework.security.git;

import com.cat2bug.system.domain.git.GitRepositoryRef;

/**
 * {@link GitPackAuthorizationFilter} 鉴权通过后附加到请求属性（{@link #REQUEST_ATTRIBUTE}）的上下文，
 * 供 admin 模块的 Smart HTTP 控制器直接使用，避免控制器重复查库/查权限（单一鉴权关卡原则）。
 */
public final class GitPackRequestContext
{
    public static final String REQUEST_ATTRIBUTE = "cat2bug.git.packContext";

    private final GitRepositoryRef repository;

    private final boolean forcePushAllowed;

    public GitPackRequestContext(GitRepositoryRef repository, boolean forcePushAllowed)
    {
        this.repository = repository;
        this.forcePushAllowed = forcePushAllowed;
    }

    public GitRepositoryRef getRepository()
    {
        return repository;
    }

    /**
     * 是否允许本次请求进行非快进（force-push）更新；仅 manage 角色（create-by/admin）为 true。
     */
    public boolean isForcePushAllowed()
    {
        return forcePushAllowed;
    }
}
