package com.cat2bug.framework.security.git;

import com.cat2bug.framework.security.git.GitSmartHttpPathSupport.GitSmartHttpPathMatch;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitSmartHttpPathSupportTest
{
    @Test
    void parsePackPath_legacyUploadPack()
    {
        Optional<GitSmartHttpPathMatch> match = GitSmartHttpPathSupport.parsePackPath(
                "/git/p/42/backend.git/git-upload-pack");
        assertTrue(match.isPresent());
        assertTrue(match.get().isLegacy());
        assertEquals(42L, match.get().getProjectId());
        assertEquals("backend", match.get().getSlug());
        assertEquals("git-upload-pack", match.get().getEndpoint());
    }

    @Test
    void parsePackPath_namespacedInfoRefs()
    {
        Optional<GitSmartHttpPathMatch> match = GitSmartHttpPathSupport.parsePackPath(
                "/acme-corp/demo-app/code/api.git/info/refs");
        assertTrue(match.isPresent());
        assertFalse(match.get().isLegacy());
        assertEquals("acme-corp", match.get().getTeamKey());
        assertEquals("demo-app", match.get().getProjectKey());
        assertEquals("api", match.get().getSlug());
        assertEquals("info/refs", match.get().getEndpoint());
    }

    @Test
    void matchesGitSecurityChain_includesNamespacedPackPath()
    {
        assertTrue(GitSmartHttpPathSupport.matchesGitSecurityChain(
                "/my-team/my-project/code/web.git/git-receive-pack"));
        assertFalse(GitSmartHttpPathSupport.matchesGitSecurityChain("/my-team/my-project/dashboard"));
    }
}
