package com.cat2bug.system.service.git;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitWebhookSigningTest
{
    @Test
    public void signatureHeaderValue_verifiesWithSameSecretAndBody()
    {
        String secret = "whsec_test_secret_value_12345678";
        String body = "{\"ref\":\"refs/heads/main\",\"repository\":{\"name\":\"demo\"}}";
        String header = GitWebhookSigning.signatureHeaderValue(secret, body);
        assertTrue(header.startsWith("sha256="));
        assertTrue(GitWebhookSigning.verify(secret, body, header));
    }

    @Test
    public void verify_rejectsWrongSecret()
    {
        String body = "{\"zen\":\"ping\"}";
        String header = GitWebhookSigning.signatureHeaderValue("secret-a-12345678", body);
        assertFalse(GitWebhookSigning.verify("secret-b-12345678", body, header));
    }
}
