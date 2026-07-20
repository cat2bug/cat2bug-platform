package com.cat2bug.system.service.git;

import com.cat2bug.common.core.domain.entity.SysUser;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitCommitIdentityMatcherTest
{
    @Test
    public void matches_byEmail_ignoreCase()
    {
        SysUser user = user("demo", "演示", "demo@example.com");
        PersonIdent person = person("t", "DEMO@example.com");
        assertTrue(GitCommitIdentityMatcher.matches(person, user));
    }

    @Test
    public void matches_byUserName()
    {
        SysUser user = user("demo", "演示", null);
        PersonIdent person = person("Demo", "other@x.com");
        assertTrue(GitCommitIdentityMatcher.matches(person, user));
    }

    @Test
    public void matches_byNickName()
    {
        SysUser user = user("demo", "演示账号", "a@b.com");
        PersonIdent person = person("演示账号", "x@y.com");
        assertTrue(GitCommitIdentityMatcher.matches(person, user));
    }

    @Test
    public void rejects_unrelatedAuthor()
    {
        SysUser user = user("demo", "演示", "demo@example.com");
        PersonIdent person = person("t", "t@local");
        assertFalse(GitCommitIdentityMatcher.matches(person, user));
    }

    @Test
    public void rejects_nulls()
    {
        assertFalse(GitCommitIdentityMatcher.matches(null, user("demo", "d", "a@b.c")));
        assertFalse(GitCommitIdentityMatcher.matches(person("demo", "a@b.c"), null));
    }

    private static SysUser user(String userName, String nickName, String email)
    {
        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setNickName(nickName);
        user.setEmail(email);
        return user;
    }

    private static PersonIdent person(String name, String email)
    {
        return new PersonIdent(name, email, new Date(0L), TimeZone.getTimeZone("UTC"));
    }
}
