package com.cat2bug.system.service.git;

import jakarta.enterprise.context.ApplicationScoped;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.common.domain.git.SysGitRemoteListRequest;
import com.cat2bug.common.domain.git.SysGitRemoteRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 调用 GitHub / GitLab / Gitee REST API 列仓（PAT 仅请求作用域，不落库）。
 */
@ApplicationScoped
public class GitRemoteRepositoryClient
{
    private static final String PROVIDER_GITHUB = "github";
    private static final String PROVIDER_GITLAB = "gitlab";
    private static final String PROVIDER_GITEE = "gitee";
    private static final String DEFAULT_GITLAB_HOST = "https://gitlab.com";
    private static final int PER_PAGE = 100;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;

    public GitRemoteRepositoryClient()
    {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    /** 测试可注入 mock/client */
    GitRemoteRepositoryClient(HttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public List<SysGitRemoteRepository> listRepositories(SysGitRemoteListRequest request)
    {
        if (request == null || StringUtils.isEmpty(request.getProvider()))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.provider-required"));
        }
        if (StringUtils.isEmpty(request.getToken()))
        {
            throw new ApiException(MessageUtils.message("git.repo.import.token-required"));
        }
        String provider = request.getProvider().trim().toLowerCase(Locale.ROOT);
        String token = request.getToken().trim();
        switch (provider)
        {
            case PROVIDER_GITHUB:
                return listGitHub(token);
            case PROVIDER_GITLAB:
                String host = StringUtils.isNotEmpty(request.getHost())
                    ? GitImportUrlValidator.validateHostBase(request.getHost())
                    : DEFAULT_GITLAB_HOST;
                return listGitLab(host, token);
            case PROVIDER_GITEE:
                return listGitee(token);
            default:
                throw new ApiException(MessageUtils.message("git.repo.import.provider-unsupported"));
        }
    }

    private List<SysGitRemoteRepository> listGitHub(String token)
    {
        String url = "https://api.github.com/user/repos?per_page=" + PER_PAGE;
        String body = httpGet(url, HttpRequest.newBuilder()
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "cat2bug-platform"));
        JSONArray array = parseArray(body);
        List<SysGitRemoteRepository> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++)
        {
            JSONObject item = array.getJSONObject(i);
            if (item == null)
            {
                continue;
            }
            SysGitRemoteRepository row = new SysGitRemoteRepository();
            row.setName(item.getString("name"));
            row.setFullPath(item.getString("full_name"));
            row.setCloneUrl(item.getString("clone_url"));
            row.setDefaultBranch(item.getString("default_branch"));
            result.add(row);
        }
        return result;
    }

    private List<SysGitRemoteRepository> listGitLab(String hostBase, String token)
    {
        String url = hostBase + "/api/v4/projects?membership=true&simple=true&per_page=" + PER_PAGE;
        String body = httpGet(url, HttpRequest.newBuilder()
            .header("PRIVATE-TOKEN", token)
            .header("Accept", "application/json")
            .header("User-Agent", "cat2bug-platform"));
        JSONArray array = parseArray(body);
        List<SysGitRemoteRepository> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++)
        {
            JSONObject item = array.getJSONObject(i);
            if (item == null)
            {
                continue;
            }
            SysGitRemoteRepository row = new SysGitRemoteRepository();
            row.setName(item.getString("name"));
            row.setFullPath(item.getString("path_with_namespace"));
            String cloneUrl = item.getString("http_url_to_repo");
            if (StringUtils.isEmpty(cloneUrl))
            {
                cloneUrl = item.getString("ssh_url_to_repo");
            }
            row.setCloneUrl(cloneUrl);
            row.setDefaultBranch(item.getString("default_branch"));
            result.add(row);
        }
        return result;
    }

    private List<SysGitRemoteRepository> listGitee(String token)
    {
        String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String url = "https://gitee.com/api/v5/user/repos?access_token=" + encoded + "&per_page=" + PER_PAGE;
        String body = httpGet(url, HttpRequest.newBuilder()
            .header("Accept", "application/json")
            .header("User-Agent", "cat2bug-platform"));
        JSONArray array = parseArray(body);
        List<SysGitRemoteRepository> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++)
        {
            JSONObject item = array.getJSONObject(i);
            if (item == null)
            {
                continue;
            }
            SysGitRemoteRepository row = new SysGitRemoteRepository();
            row.setName(item.getString("name"));
            String fullName = item.getString("full_name");
            if (StringUtils.isEmpty(fullName) && item.getJSONObject("namespace") != null)
            {
                String path = item.getJSONObject("namespace").getString("path");
                fullName = (path != null ? path + "/" : "") + item.getString("path");
            }
            row.setFullPath(fullName);
            String cloneUrl = firstNonEmpty(item.getString("html_url"), item.getString("ssh_url"));
            if (StringUtils.isNotEmpty(cloneUrl) && cloneUrl.startsWith("http") && !cloneUrl.endsWith(".git"))
            {
                cloneUrl = cloneUrl + ".git";
            }
            row.setCloneUrl(cloneUrl);
            row.setDefaultBranch(item.getString("default_branch"));
            result.add(row);
        }
        return result;
    }

    private String httpGet(String url, HttpRequest.Builder builder)
    {
        try
        {
            HttpRequest request = builder
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 401 || status == 403)
            {
                throw new ApiException(MessageUtils.message("git.repo.import.auth-failed"));
            }
            if (status < 200 || status >= 300)
            {
                throw new ApiException(MessageUtils.message("git.repo.import.remote-list-failed"))
                    .setDetailMessage("HTTP " + status);
            }
            return response.body();
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            throw new ApiException(MessageUtils.message("git.repo.import.network-failed"))
                .setDetailMessage(e.getClass().getSimpleName());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new ApiException(MessageUtils.message("git.repo.import.remote-list-failed"));
        }
        catch (IllegalArgumentException e)
        {
            throw new ApiException(MessageUtils.message("git.repo.import.host-invalid"));
        }
    }

    private static JSONArray parseArray(String body)
    {
        try
        {
            Object parsed = JSON.parse(body);
            if (parsed instanceof JSONArray)
            {
                return (JSONArray) parsed;
            }
            throw new ApiException(MessageUtils.message("git.repo.import.remote-list-failed"));
        }
        catch (ApiException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ApiException(MessageUtils.message("git.repo.import.remote-list-failed"));
        }
    }

    private static String firstNonEmpty(String a, String b)
    {
        if (StringUtils.isNotEmpty(a))
        {
            return a;
        }
        return b;
    }
}
