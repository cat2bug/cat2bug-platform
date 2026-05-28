package com.cat2bug.web.service.setup;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupRedisTestRequest;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Redis 短超时连接测试
 */
@Service
public class SetupRedisTestService
{
    private static final int TIMEOUT_MS = 3000;

    public Map<String, Object> test(SetupRedisTestRequest request)
    {
        Map<String, Object> result = new HashMap<>(2);
        if (request == null || StringUtils.isEmpty(request.getHost()) || request.getPort() == null)
        {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.redis.host.empty"));
            return result;
        }
        int database = request.getDatabase() != null ? request.getDatabase() : 0;
        try (Jedis jedis = new Jedis(request.getHost(), request.getPort(), TIMEOUT_MS))
        {
            if (StringUtils.isNotEmpty(request.getPassword()))
            {
                jedis.auth(request.getPassword());
            }
            jedis.select(database);
            String pong = jedis.ping();
            if ("PONG".equalsIgnoreCase(pong))
            {
                result.put("success", true);
                result.put("message", SetupMessages.msg("setup.test.connection.success"));
            }
            else
            {
                result.put("success", false);
                result.put("message", SetupMessages.msg("setup.test.redis.ping.invalid", pong));
            }
        }
        catch (Exception e)
        {
            result.put("success", false);
            result.put("message", formatConnectionError(e));
        }
        return result;
    }

    private static String formatConnectionError(Exception e)
    {
        Throwable root = e;
        while (root.getCause() != null)
        {
            root = root.getCause();
        }
        String detail = root.getMessage() != null ? root.getMessage() : e.getMessage();
        String lower = detail != null ? detail.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("failed to connect to any host resolved for dns name"))
        {
            return SetupMessages.msg("setup.test.redis.dns.failed");
        }
        if (lower.contains("unknown host") || lower.contains("nodename nor servname provided"))
        {
            return SetupMessages.msg("setup.test.redis.host.unknown");
        }
        if (lower.contains("connection refused") || lower.contains("connection reset"))
        {
            return SetupMessages.msg("setup.test.redis.refused");
        }
        if (lower.contains("timed out") || lower.contains("timeout"))
        {
            return SetupMessages.msg("setup.test.redis.timeout");
        }
        if (lower.contains("noauth") || lower.contains("without any password configured"))
        {
            return SetupMessages.msg("setup.test.redis.auth.required");
        }
        if (lower.contains("wrongpass") || lower.contains("invalid password"))
        {
            return SetupMessages.msg("setup.test.redis.auth.wrong");
        }
        if (lower.contains("could not connect"))
        {
            return SetupMessages.msg("setup.test.redis.connect.failed");
        }
        return SetupMessages.msg("setup.test.connection.failed", detail != null ? detail : e.getClass().getSimpleName());
    }
}
