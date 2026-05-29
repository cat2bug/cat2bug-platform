package com.cat2bug.web.service.setup;

import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupConfigWriterTest
{
    private SetupConfigWriter writer;

    private InstallProperties installProperties;

    @BeforeEach
    void setUp()
    {
        writer = new SetupConfigWriter();
        installProperties = new InstallProperties();
        ReflectionTestUtils.setField(writer, "installProperties", installProperties);
    }

    @Test
    void write_h2Local_includesDruidPoolAndCompletedFlag(@TempDir Path dir) throws Exception
    {
        installProperties.setConfigPath(dir.resolve("application-install.yml").toString());
        SetupSubmitRequest request = baseRequest("h2", "local");

        writer.write(request);

        Map<String, Object> root = readYaml(installProperties.resolveConfigPath());
        assertEquals(true, nested(root, "cat2bug", "install", "completed"));
        assertEquals("h2", nested(root, "spring", "database-type"));
        assertNotNull(nested(root, "spring", "datasource", "druid", "maxActive"));
        assertNull(nested(root, "spring", "redis"));
    }

    @Test
    void write_mysqlRedis_includesConnectionAndPoolSettings(@TempDir Path dir) throws Exception
    {
        installProperties.setConfigPath(dir.resolve("application-install.yml").toString());
        SetupSubmitRequest request = baseRequest("mysql", "redis");
        request.setMysqlHost("127.0.0.1");
        request.setMysqlPort(3306);
        request.setMysqlDatabase("cat2bug_platform");
        request.setMysqlUsername("root");
        request.setMysqlPassword("secret");
        request.setRedisHost("127.0.0.1");
        request.setRedisPort(6379);

        writer.write(request);

        Map<String, Object> root = readYaml(installProperties.resolveConfigPath());
        assertEquals(true, nested(root, "cat2bug", "install", "completed"));
        assertEquals("mysql", nested(root, "spring", "database-type"));
        assertTrue(String.valueOf(nested(root, "spring", "datasource", "druid", "master", "url")).contains("cat2bug_platform"));
        assertEquals("127.0.0.1", nested(root, "spring", "redis", "host"));
        assertNotNull(nested(root, "spring", "datasource", "druid", "initialSize"));
    }

    private static SetupSubmitRequest baseRequest(String databaseType, String cacheType)
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setDatabaseType(databaseType);
        request.setCacheType(cacheType);
        request.setProfile("uploadPath");
        request.setTemp("uploadTemp");
        request.setLogPath("./logs");
        request.setAiEnabled(false);
        return request;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readYaml(Path path) throws Exception
    {
        Object loaded = new Yaml().load(Files.readString(path));
        return (Map<String, Object>) loaded;
    }

    @SuppressWarnings("unchecked")
    private static Object nested(Map<String, Object> root, String... path)
    {
        Object current = root;
        for (String key : path)
        {
            if (!(current instanceof Map<?, ?> map))
            {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
        }
        return current;
    }
}
