package com.cat2bug.framework.config;

import com.cat2bug.common.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class Cat2BugProfileConfig {

    @ConfigProperty(name = "cat2bug.profile", defaultValue = "./upload")
    String profile;

    public Path profileRoot() {
        return Paths.get(profile).toAbsolutePath().normalize();
    }

    public Path resolveUploadPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ApiException(404, "文件不存在");
        }
        String normalized = relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            throw new ApiException(403, "非法路径");
        }
        Path file = profileRoot().resolve(normalized).normalize();
        if (!file.startsWith(profileRoot())) {
            throw new ApiException(403, "非法路径");
        }
        return file;
    }
}
