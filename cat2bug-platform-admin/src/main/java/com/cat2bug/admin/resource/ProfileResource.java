package com.cat2bug.admin.resource;

import com.cat2bug.framework.config.Cat2BugProfileConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;

@Path("/profile")
public class ProfileResource {

    @Inject
    Cat2BugProfileConfig profileConfig;

    @GET
    @Path("{filePath:.+}")
    public Response getFile(@PathParam("filePath") String filePath) throws IOException {
        java.nio.file.Path file = profileConfig.resolveUploadPath(filePath);
        if (!Files.isRegularFile(file)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return Response.ok(Files.readAllBytes(file))
                .header("Content-Type", contentType)
                .build();
    }
}
