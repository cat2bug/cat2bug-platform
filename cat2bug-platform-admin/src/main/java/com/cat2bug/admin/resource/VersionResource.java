package com.cat2bug.admin.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 与 Spring {@code SysVersionController} 对齐：返回纯文本版本号。
 */
@Path("/version")
public class VersionResource {

    @ConfigProperty(name = "cat2bug.version", defaultValue = "1.0.0")
    String version;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return version;
    }
}
