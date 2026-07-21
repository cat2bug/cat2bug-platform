package com.cat2bug.system.git.browse.support;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class GitContributorSupportTest
{
    @Test
    public void aggregationKey_prefersNormalizedEmail()
    {
        assertEquals("e:alice@example.com", GitContributorSupport.aggregationKey("Alice", "Alice@Example.com"));
        assertEquals("e:alice@example.com", GitContributorSupport.aggregationKey("Bob", " alice@example.com "));
    }

    @Test
    public void aggregationKey_sameEmailDifferentNames_collapses()
    {
        assertEquals(
                GitContributorSupport.aggregationKey("Alice A", "a@x.com"),
                GitContributorSupport.aggregationKey("Alice B", "a@x.com"));
    }

    @Test
    public void aggregationKey_fallsBackToNameWhenEmailMissing()
    {
        assertEquals("n:ghost", GitContributorSupport.aggregationKey("Ghost", ""));
        assertEquals("n:ghost", GitContributorSupport.aggregationKey("  Ghost  ", null));
    }

    @Test
    public void aggregationKey_distinctEmails_distinctKeys()
    {
        assertNotEquals(
                GitContributorSupport.aggregationKey("A", "a@x.com"),
                GitContributorSupport.aggregationKey("B", "b@x.com"));
    }

    @Test
    public void aggregationKey_nameOnly_sameNameSameKey()
    {
        assertEquals(
                GitContributorSupport.aggregationKey("Solo", null),
                GitContributorSupport.aggregationKey("solo", ""));
    }
}
