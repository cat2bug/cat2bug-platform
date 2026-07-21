package com.cat2bug.system.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SysGitRepositoryCloneUrlTest
{
    @Test
    public void getCloneUrl_returnsNamespacedPath()
    {
        SysGitRepository repo = new SysGitRepository();
        repo.setTeamKey("acme");
        repo.setProjectKey("demo");
        repo.setSlug("backend");
        assertEquals("/acme/demo/code/backend.git", repo.getCloneUrl());
    }

    @Test
    public void getCloneUrl_missingKeys_returnsNull()
    {
        SysGitRepository repo = new SysGitRepository();
        repo.setProjectId(1L);
        repo.setSlug("backend");
        assertNull(repo.getCloneUrl());
    }
}
