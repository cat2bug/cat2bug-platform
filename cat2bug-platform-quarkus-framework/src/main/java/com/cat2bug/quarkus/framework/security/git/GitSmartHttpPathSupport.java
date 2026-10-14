package com.cat2bug.quarkus.framework.security.git;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Smart HTTP 路径解析：legacy {@code /git/p/{projectId}/{slug}.git/**} 与
 * namespaced {@code /{teamKey}/{projectKey}/code/{slug}.git/**}。
 */
public final class GitSmartHttpPathSupport
{
    private static final Pattern LEGACY_PACK_PATH = Pattern.compile(
            "^/git/p/(\\d+)/([^/]+)\\.git/(info/refs|git-upload-pack|git-receive-pack)$");

    private static final Pattern NAMESPACED_PACK_PATH = Pattern.compile(
            "^/([^/]+)/([^/]+)/code/([^/]+)\\.git/(info/refs|git-upload-pack|git-receive-pack)$");

    private GitSmartHttpPathSupport()
    {
    }

    public static boolean matchesNamespacedPackPath(String path)
    {
        return path != null && NAMESPACED_PACK_PATH.matcher(path).matches();
    }

    /**
     * 是否应由 Git Basic 安全链处理（{@code /git/**} 或 namespaced pack 端点）。
     */
    public static boolean matchesGitSecurityChain(String path)
    {
        if (path == null)
        {
            return false;
        }
        if (path.startsWith("/git/"))
        {
            return true;
        }
        return matchesNamespacedPackPath(path);
    }

    public static Optional<GitSmartHttpPathMatch> parsePackPath(String path)
    {
        if (path == null)
        {
            return Optional.empty();
        }
        Matcher legacy = LEGACY_PACK_PATH.matcher(path);
        if (legacy.matches())
        {
            return Optional.of(GitSmartHttpPathMatch.legacy(
                    Long.valueOf(legacy.group(1)),
                    legacy.group(2),
                    legacy.group(3)));
        }
        Matcher namespaced = NAMESPACED_PACK_PATH.matcher(path);
        if (namespaced.matches())
        {
            return Optional.of(GitSmartHttpPathMatch.namespaced(
                    namespaced.group(1),
                    namespaced.group(2),
                    namespaced.group(3),
                    namespaced.group(4)));
        }
        return Optional.empty();
    }

    public static final class GitSmartHttpPathMatch
    {
        private final Long projectId;
        private final String teamKey;
        private final String projectKey;
        private final String slug;
        private final String endpoint;

        private GitSmartHttpPathMatch(Long projectId, String teamKey, String projectKey, String slug, String endpoint)
        {
            this.projectId = projectId;
            this.teamKey = teamKey;
            this.projectKey = projectKey;
            this.slug = slug;
            this.endpoint = endpoint;
        }

        static GitSmartHttpPathMatch legacy(Long projectId, String slug, String endpoint)
        {
            return new GitSmartHttpPathMatch(projectId, null, null, slug, endpoint);
        }

        static GitSmartHttpPathMatch namespaced(String teamKey, String projectKey, String slug, String endpoint)
        {
            return new GitSmartHttpPathMatch(null, teamKey, projectKey, slug, endpoint);
        }

        public boolean isLegacy()
        {
            return projectId != null;
        }

        public Long getProjectId()
        {
            return projectId;
        }

        public String getTeamKey()
        {
            return teamKey;
        }

        public String getProjectKey()
        {
            return projectKey;
        }

        public String getSlug()
        {
            return slug;
        }

        public String getEndpoint()
        {
            return endpoint;
        }
    }
}
