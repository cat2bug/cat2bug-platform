package com.cat2bug.web.service.setup;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupPathTestRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 目录存在且可写测试
 */
@Service
public class SetupPathTestService
{
    public Map<String, Object> test(SetupPathTestRequest request)
    {
        Map<String, Object> result = new HashMap<>(2);
        if (request == null || StringUtils.isEmpty(request.getPath()))
        {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.path.empty"));
            return result;
        }
        try
        {
            Path dir = Paths.get(request.getPath()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path probe = dir.resolve(".cat2bug-write-test");
            Files.writeString(probe, "ok");
            Files.deleteIfExists(probe);
            result.put("success", true);
            result.put("message", SetupMessages.msg("setup.test.path.writable"));
        }
        catch (IOException e)
        {
            result.put("success", false);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            result.put("message", SetupMessages.msg("setup.test.path.not.writable", detail));
        }
        return result;
    }
}
