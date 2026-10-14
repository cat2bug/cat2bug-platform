package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 云端 AI 用例检索，对齐 Spring {@code CloudCaseServiceImpl}。
 */
@ApplicationScoped
public class CloudCaseService {

    private static final Logger LOG = Logger.getLogger(CloudCaseService.class);

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "cat2bug.cloud.host", defaultValue = "https://www.cat2bug.com/cloud/api")
    String cloudBaseUrl;

    @ConfigProperty(name = "cat2bug.cloud.timeout-seconds", defaultValue = "60")
    int timeoutSeconds;

    public List<SysCase> searchCaseListOfAi(String content) {
        String base = cloudBaseUrl.endsWith("/") ? cloudBaseUrl.substring(0, cloudBaseUrl.length() - 1) : cloudBaseUrl;
        StringBuilder url = new StringBuilder(base).append("/ai/case/list");
        if (StringUtils.isNotBlank(content)) {
            url.append("?content=").append(URLEncoder.encode(content.trim(), StandardCharsets.UTF_8));
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 30)))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.debugf("cloud case search status=%d url=%s", response.statusCode(), url);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warnf("云端用例检索失败 HTTP %d", response.statusCode());
                return Collections.emptyList();
            }
            String body = response.body();
            if (StringUtils.isBlank(body)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(body, new TypeReference<List<SysCase>>() {
            });
        } catch (Exception e) {
            LOG.warnf("云端用例检索异常: %s", e.getMessage());
            return Collections.emptyList();
        }
    }
}
