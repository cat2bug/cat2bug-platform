package com.cat2bug.quarkus.admin.config;

import com.cat2bug.quarkus.common.config.InstallConfigSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallRuntimePropertiesConfigSourceTest {

    @Test
    void loadsRuntimePropertiesFromInstallDirectory(@TempDir Path tempDir) throws Exception {
        Path runtime = tempDir.resolve("quarkus-runtime.properties");
        Files.writeString(runtime, """
                quarkus.datasource.runtime-mysql.active=true
                quarkus.mybatis.runtime-mysql.database-id=mysql
                """);

        Map<String, String> props = InstallRuntimePropertiesConfigSource.loadPropertiesFromPath(runtime);
        assertEquals("true", props.get("quarkus.datasource.runtime-mysql.active"));
        assertEquals("mysql", props.get("quarkus.mybatis.runtime-mysql.database-id"));
    }

    @Test
    void returnsEmptyWhenRuntimeFileMissing(@TempDir Path tempDir) {
        Path runtime = tempDir.resolve("missing-quarkus-runtime.properties");
        assertTrue(InstallRuntimePropertiesConfigSource.loadPropertiesFromPath(runtime).isEmpty());
    }
}
