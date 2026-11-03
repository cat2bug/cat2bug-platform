package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysProjectPush;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 云站 multipart session 客户端。
 *
 * <p>与 cloud {@code OpenApiProjectResource} 对齐：
 * <ul>
 *   <li>{@code POST {cloud}/api/project/push/session} JSON 创建会话（返回 sessionId）</li>
 *   <li>{@code POST {cloud}/api/project/push} multipart 上传 part（头 {@code X-Cat2bug-Push-*}）</li>
 *   <li>旧单包：同路径但不带 session/part 头</li>
 * </ul>
 */
@ApplicationScoped
public class CloudPushMultipartClient {

    private static final Logger LOG = Logger.getLogger(CloudPushMultipartClient.class);

    public static final String HEADER_API_KEY = "CAT2BUG-API-KEY";
    public static final String HEADER_SESSION = "X-Cat2bug-Push-Session";
    public static final String HEADER_PART = "X-Cat2bug-Push-Part";
    public static final String HEADER_PART_TYPE = "X-Cat2bug-Push-Part-Type";
    public static final String HEADER_SHA256 = "X-Cat2bug-Push-Part-Sha256";
    public static final String HEADER_PART_INDEX = "X-Cat2bug-Push-Part-Index";
    public static final String HEADER_PART_COUNT = "X-Cat2bug-Push-Part-Count";

    @ConfigProperty(name = "cat2bug.cloud.host", defaultValue = "https://cloud.cat2bug.com/prod-api")
    String cloudUrl;

    @ConfigProperty(name = "cat2bug.version", defaultValue = "1.0.0")
    String systemVersion;

    public static final class SessionOpen {
        public final String sessionId;
        public final boolean legacyReceiveUrl;

        SessionOpen(String sessionId, boolean legacyReceiveUrl) {
            this.sessionId = sessionId;
            this.legacyReceiveUrl = legacyReceiveUrl;
        }
    }

    public SessionOpen openSession(String pullKey, String sessionId, Map<String, Object> manifest,
                                   Map<String, Object> include) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", systemVersion);
        body.put("packageSchema", SysProjectPush.PACKAGE_SCHEMA_1_2);
        body.put("sessionId", sessionId);
        body.put("include", include);
        body.put("parts", manifest.get("parts"));
        body.put("manifest", manifest);
        String json = CloudPushSupport.JSON.writeValueAsString(body);
        String url = trimSlash(cloudUrl) + "/api/project/push/session";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header(HEADER_API_KEY, pullKey)
                .header(HEADER_SESSION, sessionId)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResult result = send(request);
        if (result.status == 404 || result.status == 405 || result.status == 501) {
            LOG.infof("云站未提供 /push/session（HTTP %d），改由首个 part 携带 manifest 建会话", result.status);
            return new SessionOpen(sessionId, true);
        }
        if (result.status == 409) {
            String adopted = readSessionId(result.body, sessionId);
            return new SessionOpen(adopted, false);
        }
        if (result.status < 200 || result.status >= 300) {
            throw new IOException("创建云站同步会话失败，" + errorDetail(result.status, result.body));
        }
        // cloud 返回的 sessionId 与 projectPushId 相同，后续上传必须用它
        return new SessionOpen(readSessionId(result.body, sessionId), false);
    }

    /**
     * 上传一个分片。返回云站会话 ID（与 projectPushId 相同）；调用方应写回 job，供后续分片使用。
     * {@code sessionId} 为空时表示首包建会话（不发 Session 头）。
     */
    public String uploadPart(String pullKey, String sessionId,
                             String partId, String partType, String sha256,
                             int partIndex, int partCount, String manifestJson, Path file) throws IOException {
        String url = trimSlash(cloudUrl) + "/api/project/push";
        return postPart(url, pullKey, sessionId, partId, partType, sha256, partIndex, partCount, manifestJson, file);
    }

    /** 旧云站无 session 接口时：一次上传合并后的单 zip，入站列表只产生一条。 */
    public void uploadLegacyZip(String pullKey, Path file) throws IOException {
        String url = trimSlash(cloudUrl) + "/api/project/push";
        String boundary = "----Cat2Bug" + UUID.randomUUID().toString().replace("-", "");
        String fileName = file.getFileName() == null ? "project.zip" : file.getFileName().toString();
        StringBuilder preamble = new StringBuilder();
        field(preamble, boundary, "version", systemVersion);
        preamble.append("--").append(boundary).append("\r\n");
        preamble.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        preamble.append("Content-Type: application/zip\r\n\r\n");
        byte[] epilogue = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        Path bodyFile = Files.createTempFile("cat2bug-push-legacy-", ".multipart");
        try {
            try (var out = Files.newOutputStream(bodyFile)) {
                out.write(preamble.toString().getBytes(StandardCharsets.UTF_8));
                Files.copy(file, out);
                out.write(epilogue);
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header(HEADER_API_KEY, pullKey)
                    .timeout(Duration.ofMinutes(60))
                    .POST(HttpRequest.BodyPublishers.ofFile(bodyFile))
                    .build();
            HttpResult result = send(request);
            if (result.status < 200 || result.status >= 300) {
                throw new IOException("上传同步包失败，" + errorDetail(result.status, result.body));
            }
        } finally {
            try {
                Files.deleteIfExists(bodyFile);
            } catch (IOException ignored) {
                // 临时文件清理失败不影响结果
            }
        }
    }

    private String postPart(String url, String pullKey, String sessionId, String partId, String partType,
                            String sha256, int partIndex, int partCount, String manifestJson, Path file)
            throws IOException {
        String boundary = "----Cat2Bug" + UUID.randomUUID().toString().replace("-", "");
        String fileName = partId + ".zip";
        StringBuilder preamble = new StringBuilder();
        field(preamble, boundary, "version", systemVersion);
        if (sessionId != null && !sessionId.isBlank()) {
            field(preamble, boundary, "sessionId", sessionId);
        }
        field(preamble, boundary, "partId", partId);
        field(preamble, boundary, "partType", partType);
        field(preamble, boundary, "sha256", sha256 == null ? "" : sha256);
        field(preamble, boundary, "partIndex", Integer.toString(partIndex));
        field(preamble, boundary, "partCount", Integer.toString(partCount));
        field(preamble, boundary, "manifest", manifestJson == null ? "{}" : manifestJson);
        preamble.append("--").append(boundary).append("\r\n");
        preamble.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        preamble.append("Content-Type: application/zip\r\n\r\n");
        byte[] epilogue = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        Path bodyFile = Files.createTempFile("cat2bug-push-part-", ".multipart");
        try {
            try (var out = Files.newOutputStream(bodyFile)) {
                out.write(preamble.toString().getBytes(StandardCharsets.UTF_8));
                Files.copy(file, out);
                out.write(epilogue);
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header(HEADER_API_KEY, pullKey)
                    .header(HEADER_PART, partId)
                    .header(HEADER_PART_TYPE, partType)
                    .header(HEADER_SHA256, sha256 == null ? "" : sha256)
                    .header(HEADER_PART_INDEX, Integer.toString(partIndex))
                    .header(HEADER_PART_COUNT, Integer.toString(partCount))
                    .timeout(Duration.ofMinutes(60))
                    .POST(HttpRequest.BodyPublishers.ofFile(bodyFile));
            if (sessionId != null && !sessionId.isBlank()) {
                builder.header(HEADER_SESSION, sessionId);
            }
            HttpResult result = send(builder.build());
            if (result.status < 200 || result.status >= 300) {
                throw new IOException("上传分包 " + partId + " 失败，" + errorDetail(result.status, result.body));
            }
            return readSessionId(result.body, sessionId);
        } finally {
            try {
                Files.deleteIfExists(bodyFile);
            } catch (IOException ignored) {
                // 临时文件清理失败不影响结果
            }
        }
    }

    private static void field(StringBuilder preamble, String boundary, String name, String value) {
        preamble.append("--").append(boundary).append("\r\n");
        preamble.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        preamble.append(value == null ? "" : value).append("\r\n");
    }

    private static HttpResult send(HttpRequest request) throws IOException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new HttpResult(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("上传被中断", e);
        }
    }

    static String readSessionId(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            JsonNode node = CloudPushSupport.JSON.readTree(body);
            JsonNode data = node.has("data") && node.get("data").isObject() ? node.get("data") : node;
            for (String key : new String[] {"sessionId", "projectPushId"}) {
                if (data.hasNonNull(key)) {
                    String id = data.get(key).asText();
                    if (id != null && !id.isBlank()) {
                        return id.trim();
                    }
                }
            }
        } catch (Exception ignored) {
            // 非 JSON 时沿用调用方传入的 fallback
        }
        return fallback;
    }

    static String errorDetail(int status, String body) {
        String msg = null;
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = CloudPushSupport.JSON.readTree(body);
                if (node.hasNonNull("msg")) {
                    msg = node.get("msg").asText();
                } else if (node.hasNonNull("message")) {
                    msg = node.get("message").asText();
                }
            } catch (Exception ignored) {
                msg = body.trim();
            }
        }
        if (msg == null) {
            msg = "";
        }
        msg = msg.replaceAll("\\s+", " ").trim();
        if (msg.length() > 300) {
            msg = msg.substring(0, 300);
        }
        return msg.isEmpty() ? ("HTTP " + status) : ("HTTP " + status + "：" + msg);
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        String value = url.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record HttpResult(int status, String body) {
    }
}
