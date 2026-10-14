package com.cat2bug.quarkus.admin.setup;

import com.cat2bug.quarkus.common.domain.setup.SetupOllamaTestRequest;
import com.cat2bug.quarkus.common.setup.SetupMessages;
import com.cat2bug.quarkus.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SetupOllamaTestService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public Map<String, Object> test(SetupOllamaTestRequest request) {
        Map<String, Object> result = new HashMap<>(2);
        if (request == null || StringUtils.isEmpty(request.getHost())) {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.ollama.host.empty"));
            return result;
        }
        String base = request.getHost().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/tags";
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                result.put("success", true);
                result.put("message", SetupMessages.msg("setup.test.connection.success"));
            } else {
                result.put("success", false);
                result.put("message", SetupMessages.msg("setup.test.ollama.connection.failed", "HTTP " + response.statusCode()));
            }
        } catch (Exception e) {
            result.put("success", false);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            result.put("message", SetupMessages.msg("setup.test.ollama.connection.failed", detail));
        }
        return result;
    }
}
