package com.cat2bug.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallConfigSupportTest
{
    @Test
    void locateExistingConfigFile_fallsBackToJarDirectory(@TempDir Path tempDir) throws Exception
    {
        Path jarDir = tempDir.resolve("jar-home");
        String relative = "config/install/test-" + UUID.randomUUID() + ".yml";
        Path jarConfig = jarDir.resolve(relative);
        Files.createDirectories(jarConfig.getParent());
        Files.writeString(jarConfig, "cat2bug:\n  install:\n    completed: true\n");

        Optional<Path> located = InstallConfigSupport.locateExistingConfigFile(relative, jarDir);
        assertTrue(located.isPresent());
        assertEquals(jarConfig.toAbsolutePath().normalize(), located.orElseThrow());
    }

    @Test
    void locateExistingConfigFile_prefersWorkingDirectory(@TempDir Path tempDir) throws Exception
    {
        Path jarDir = tempDir.resolve("jar-home");
        Path workingDir = tempDir.resolve("cwd");
        String relative = "config/install/test-" + UUID.randomUUID() + ".yml";
        Path cwdConfig = workingDir.resolve(relative);
        Path jarConfig = jarDir.resolve(relative);
        Files.createDirectories(cwdConfig.getParent());
        Files.createDirectories(jarConfig.getParent());
        Files.writeString(cwdConfig, "cat2bug:\n  install:\n    completed: true\n");
        Files.writeString(jarConfig, "cat2bug:\n  install:\n    completed: false\n");

        Optional<Path> located = InstallConfigSupport.locateExistingConfigFile(
                workingDir.resolve(relative).toString(), jarDir);
        assertTrue(located.isPresent());
        assertEquals(cwdConfig.toAbsolutePath().normalize(), located.orElseThrow());
    }

    @Test
    void locateExistingConfigFile_doesNotFallbackForAbsolutePath(@TempDir Path tempDir) throws Exception
    {
        Path jarDir = tempDir.resolve("jar-home");
        Path workingDir = tempDir.resolve("cwd");
        Files.createDirectories(workingDir);
        Files.createDirectories(jarDir.resolve("config/install"));
        Path jarConfig = jarDir.resolve("config/install/application-install.yml");
        Files.writeString(jarConfig, "cat2bug:\n  install:\n    completed: true\n");
        Path missingAbsolute = workingDir.resolve("missing-install.yml").toAbsolutePath();

        assertFalse(InstallConfigSupport.locateExistingConfigFile(
                missingAbsolute.toString(), jarDir).isPresent());
    }

    @Test
    void toLocalPath_parsesNestedJarUri() throws Exception
    {
        URI nestedJar = URI.create("jar:file:/opt/cat2bug/cat2bug-admin.jar!/BOOT-INF/lib/common.jar!/");
        assertEquals(Path.of("/opt/cat2bug/cat2bug-admin.jar"),
                InstallConfigSupport.toLocalPath(nestedJar));
    }
}
