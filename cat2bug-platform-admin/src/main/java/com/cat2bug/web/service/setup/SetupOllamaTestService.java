package com.cat2bug.web.service.setup;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupOllamaTestRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Ollama /api/tags 可达性测试
 */
@Service
public class SetupOllamaTestService
{
    private static final int TIMEOUT_MS = 10_000;

    public Map<String, Object> test(SetupOllamaTestRequest request)
    {
        Map<String, Object> result = new HashMap<>(2);
        if (request == null || StringUtils.isEmpty(request.getHost()))
        {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.ollama.host.empty"));
            return result;
        }
        String base = request.getHost().trim();
        if (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/api/tags";
        try
        {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(TIMEOUT_MS);
            factory.setReadTimeout(TIMEOUT_MS);
            RestTemplate client = new RestTemplate(factory);
            client.getForEntity(url, String.class);
            result.put("success", true);
            result.put("message", SetupMessages.msg("setup.test.connection.success"));
        }
        catch (Exception e)
        {
            result.put("success", false);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            result.put("message", SetupMessages.msg("setup.test.ollama.connection.failed", detail));
        }
        return result;
    }
}
