package com.cat2bug.system.service.git;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.git.SysGitRemoteListRequest;
import com.cat2bug.system.domain.git.SysGitRemoteRepository;
import com.cat2bug.system.support.MessageSourceTestSupport;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link GitRemoteRepositoryClient}：GitLab 自定义 host 列仓（本地 HttpServer mock）。
 */
public class GitRemoteRepositoryClientTest
{
    private Runnable messageCleanup;
    private HttpServer server;
    private String hostBase;
    private final GitRemoteRepositoryClient client = new GitRemoteRepositoryClient();

    @Before
    public void setUp() throws IOException
    {
        messageCleanup = MessageSourceTestSupport.installMessageSource();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v4/projects", exchange -> {
            String token = exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN");
            byte[] body;
            int status;
            if (!"test-pat".equals(token))
            {
                status = 401;
                body = "{\"message\":\"401 Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
            }
            else
            {
                status = 200;
                String json = "["
                    + "{\"name\":\"demo\",\"path_with_namespace\":\"group/demo\","
                    + "\"http_url_to_repo\":\"https://gitlab.example.com/group/demo.git\","
                    + "\"default_branch\":\"main\"}"
                    + "]";
                body = json.getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(body);
            }
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        hostBase = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void tearDown()
    {
        if (server != null)
        {
            server.stop(0);
        }
        if (messageCleanup != null)
        {
            messageCleanup.run();
        }
    }

    @Test
    public void listRepositories_gitlabCustomHost_usesHostAndPrivateToken()
    {
        SysGitRemoteListRequest request = new SysGitRemoteListRequest();
        request.setProvider("gitlab");
        request.setToken("test-pat");
        request.setHost(hostBase);

        List<SysGitRemoteRepository> list = client.listRepositories(request);

        assertEquals(1, list.size());
        assertEquals("demo", list.get(0).getName());
        assertEquals("group/demo", list.get(0).getFullPath());
        assertEquals("https://gitlab.example.com/group/demo.git", list.get(0).getCloneUrl());
        assertEquals("main", list.get(0).getDefaultBranch());
    }

    @Test
    public void listRepositories_gitlabUnauthorized_mapsAuthError()
    {
        SysGitRemoteListRequest request = new SysGitRemoteListRequest();
        request.setProvider("gitlab");
        request.setToken("bad-token");
        request.setHost(hostBase);

        try
        {
            client.listRepositories(request);
            fail("expected ServiceException for 401");
        }
        catch (ServiceException expected)
        {
            assertTrue(expected.getMessage().contains("git.repo.import.auth-failed")
                || expected.getMessage().contains("i18n:git.repo.import.auth-failed"));
        }
    }

    @Test
    public void listRepositories_toStringHidesToken()
    {
        SysGitRemoteListRequest request = new SysGitRemoteListRequest();
        request.setProvider("github");
        request.setToken("super-secret-pat");
        String text = request.toString();
        assertTrue(text.contains("***"));
        assertTrue(!text.contains("super-secret-pat"));
    }
}
