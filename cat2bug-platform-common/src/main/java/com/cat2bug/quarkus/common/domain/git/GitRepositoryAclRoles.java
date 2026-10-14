package com.cat2bug.quarkus.common.domain.git;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 仓级 ACL 角色常量。
 */
public final class GitRepositoryAclRoles
{
    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final String ADMIN = "admin";

    public static final Set<String> ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            READ, WRITE, ADMIN)));

    private GitRepositoryAclRoles()
    {
    }
}
