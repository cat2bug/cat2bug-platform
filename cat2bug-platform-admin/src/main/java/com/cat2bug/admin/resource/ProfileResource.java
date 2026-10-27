package com.cat2bug.admin.resource;

import com.cat2bug.framework.config.Cat2BugProfileConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

@Path("/profile")
public class ProfileResource {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Inject
    Cat2BugProfileConfig profileConfig;

    /**
     * 本地 {@code cat2bug.profile} 缺文件时回源的站点根（如 https://www.cat2bug.com）。
     * 勿写成 {@code cat2bug.profile.remote-base}：与标量 {@code cat2bug.profile} 同前缀会冲突，SmallRye 读不到。
     */
    @ConfigProperty(name = "cat2bug.profile-remote-base", defaultValue = " ")
    String remoteBase;

    @GET
    @Path("{filePath:.+}")
    public Response getFile(@PathParam("filePath") String filePath) throws IOException {
        java.nio.file.Path file = profileConfig.resolveUploadPath(filePath);
        if (Files.isRegularFile(file)) {
            return fileResponse(Files.readAllBytes(file), contentType(file, null));
        }
        URI remote = ProfileRemoteFile.uri(remoteBase, filePath);
        if (remote == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            HttpResponse<byte[]> response = HTTP.send(
                    HttpRequest.newBuilder(remote).timeout(Duration.ofSeconds(15)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body() == null || response.body().length == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return fileResponse(response.body(), contentType(file, response.headers().firstValue("content-type").orElse(null)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (IOException | IllegalArgumentException e) {
            // 非法 URI / 回源失败 → 404，避免空格等字符触发 500
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private static Response fileResponse(byte[] body, String contentType) {
        return Response.ok(body).header("Content-Type", contentType).build();
    }

    private static String contentType(java.nio.file.Path file, String remoteType) throws IOException {
        if (remoteType != null && !remoteType.isBlank() && !remoteType.startsWith("text/html")) {
            return remoteType.split(";")[0].trim();
        }
        // 回源场景本地文件可能不存在，probe 前先判断
        if (file != null && Files.isRegularFile(file)) {
            String contentType = Files.probeContentType(file);
            if (contentType != null) {
                return contentType;
            }
        }
        return guessContentTypeByName(file == null ? null : file.getFileName().toString());
    }

    private static String guessContentTypeByName(String name) {
        if (name == null) {
            return "application/octet-stream";
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
