package com.cat2bug.system.service.git;

import com.cat2bug.system.domain.SysGitRepositoryWebhook;
import com.cat2bug.system.domain.git.SysGitRepositoryWebhookVo;
import com.cat2bug.system.service.impl.SysGitRepositoryWebhookServiceImpl;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitWebhookEventFilterTest
{
    @Test
    public void subscribes_skipsInactiveWebhook()
    {
        SysGitRepositoryWebhook row = webhook("[\"push\"]", 0);
        assertFalse(GitWebhookPayloadBuilder.subscribes(row, GitWebhookEventTypes.SUBSCRIPTION_PUSH));
    }

    @Test
    public void subscribes_skipsUnsubscribedEvent()
    {
        SysGitRepositoryWebhook row = webhook("[\"tag\"]", 1);
        assertFalse(GitWebhookPayloadBuilder.subscribes(row, GitWebhookEventTypes.SUBSCRIPTION_PUSH));
        assertTrue(GitWebhookPayloadBuilder.subscribes(row, GitWebhookEventTypes.SUBSCRIPTION_TAG));
    }

    @Test
    public void listVo_neverContainsPlaintextSecret() throws Exception
    {
        SysGitRepositoryWebhook row = webhook("[\"push\",\"pull_request\"]", 1);
        row.setWebhookId(9L);
        row.setRepositoryId(3L);
        row.setUrl("https://example.com/hook");
        row.setSecretCipher("encrypted-not-plain");

        SysGitRepositoryWebhookServiceImpl service = new SysGitRepositoryWebhookServiceImpl();
        Method toVo = SysGitRepositoryWebhookServiceImpl.class.getDeclaredMethod("toVo", SysGitRepositoryWebhook.class);
        toVo.setAccessible(true);
        SysGitRepositoryWebhookVo vo = (SysGitRepositoryWebhookVo) toVo.invoke(service, row);

        assertTrue(vo.getSecretConfigured());
        assertFalse(Arrays.stream(SysGitRepositoryWebhookVo.class.getMethods())
                .anyMatch(m -> "getSecret".equals(m.getName())));
        assertFalse(Arrays.stream(SysGitRepositoryWebhookVo.class.getDeclaredFields())
                .anyMatch(f -> "secret".equals(f.getName())));
    }

    private static SysGitRepositoryWebhook webhook(String eventsJson, int active)
    {
        SysGitRepositoryWebhook row = new SysGitRepositoryWebhook();
        row.setEvents(eventsJson);
        row.setActive(active);
        row.setSecretCipher("cipher");
        return row;
    }
}
