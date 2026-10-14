package com.cat2bug.framework.storage;

import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.config.Cat2BugProfileConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class FileDownloadHelper {

    private static final String RESOURCE_PREFIX = "/profile";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "bmp", "gif", "jpg", "jpeg", "png", "svg",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "html", "htm", "txt",
            "rar", "zip", "gz", "bz2",
            "mp4", "avi", "rmvb",
            "pdf"
    );

    @Inject
    Cat2BugProfileConfig profileConfig;

    public Response downloadResource(String resource) throws IOException {
        if (!checkAllowDownload(resource)) {
            throw new ApiException(403, "资源文件非法，不允许下载");
        }
        String relative = stripResourcePrefix(resource);
        Path file = profileConfig.resolveUploadPath(relative);
        if (!Files.isRegularFile(file)) {
            throw new ApiException(404, "文件不存在");
        }
        String downloadName = file.getFileName().toString();
        return buildFileResponse(file, downloadName);
    }

    public Response downloadByFileName(String fileName, boolean delete) throws IOException {
        if (!checkAllowDownload(fileName)) {
            throw new ApiException(403, "文件名称非法，不允许下载");
        }
        int underscore = fileName.indexOf('_');
        String displayName = underscore >= 0 ? fileName.substring(underscore + 1) : fileName;
        Path downloadRoot = profileConfig.profileRoot().resolve("download").normalize();
        Path file = downloadRoot.resolve(fileName).normalize();
        if (!file.startsWith(downloadRoot) || !Files.isRegularFile(file)) {
            throw new ApiException(404, "文件不存在");
        }
        Response response = buildFileResponse(file, displayName);
        if (delete) {
            Files.deleteIfExists(file);
        }
        return response;
    }

    public static boolean checkAllowDownload(String resource) {
        if (resource == null || resource.contains("..")) {
            return false;
        }
        String ext = extractExtension(resource);
        return !ext.isEmpty() && ALLOWED_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    private Response buildFileResponse(Path file, String downloadName) throws IOException {
        String encoded = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = "attachment; filename=" + encoded + ";filename*=utf-8''" + encoded;
        StreamingOutput stream = output -> {
            try (InputStream in = Files.newInputStream(file)) {
                in.transferTo(output);
            }
        };
        return Response.ok(stream)
                .header("Content-Disposition", disposition)
                .header("download-filename", encoded)
                .header("Access-Control-Expose-Headers", "Content-Disposition,download-filename")
                .type("application/octet-stream")
                .build();
    }

    private static String stripResourcePrefix(String resource) {
        String path = resource;
        if (path.startsWith(RESOURCE_PREFIX)) {
            path = path.substring(RESOURCE_PREFIX.length());
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    private static String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1);
    }
}
