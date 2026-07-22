package com.cat2bug.system.service.git;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * 导入 URL scheme 限制：仅 http(s)。
 */
public class GitImportUrlValidatorTest
{
    private Runnable messageCleanup;

    @Before
    public void setUp()
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
    }

    @After
    public void tearDown()
    {
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
    }

    @Test
    public void validateCloneUrl_httpsAccepted()
    {
        assertEquals("https://github.com/org/repo.git",
            GitImportUrlValidator.validateCloneUrl("https://github.com/org/repo.git"));
    }

    @Test
    public void validateCloneUrl_httpAcceptedForSelfHosted()
    {
        assertEquals("http://git.local/group/repo.git",
            GitImportUrlValidator.validateCloneUrl("http://git.local/group/repo.git"));
    }

    @Test
    public void validateCloneUrl_emptyRejected()
    {
        try
        {
            GitImportUrlValidator.validateCloneUrl("  ");
            fail("expected ServiceException");
        }
        catch (ServiceException expected)
        {
            // empty
        }
    }

    @Test
    public void validateCloneUrl_sshSchemeRejected()
    {
        try
        {
            GitImportUrlValidator.validateCloneUrl("ssh://git@github.com/org/repo.git");
            fail("expected ServiceException");
        }
        catch (ServiceException expected)
        {
            // scheme
        }
    }

    @Test
    public void validateCloneUrl_gitScpRejected()
    {
        try
        {
            GitImportUrlValidator.validateCloneUrl("git@github.com:org/repo.git");
            fail("expected ServiceException");
        }
        catch (ServiceException expected)
        {
            // malformed / no scheme
        }
    }

    @Test
    public void validateHostBase_stripsPathAndTrailingSlashSemantics()
    {
        assertEquals("https://gitlab.example.com",
            GitImportUrlValidator.validateHostBase("https://gitlab.example.com/"));
        assertEquals("https://gitlab.example.com:8443",
            GitImportUrlValidator.validateHostBase("https://gitlab.example.com:8443/foo"));
    }
}
