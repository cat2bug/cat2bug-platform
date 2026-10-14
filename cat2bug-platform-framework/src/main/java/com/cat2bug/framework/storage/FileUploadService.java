package com.cat2bug.framework.storage;

import com.cat2bug.common.exception.ApiException;
import com.cat2bug.framework.config.Cat2BugProfileConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class FileUploadService {

    private static final String RESOURCE_PREFIX = "/profile";
    private static final long DEFAULT_MAX_SIZE = 500L * 1024 * 1024;
    private static final Pattern BASE64_IMAGE_PATTERN =
            Pattern.compile("^data:image/([a-zA-Z]+);base64,(.+)$");

    @Inject
    Cat2BugProfileConfig profileConfig;

    public UploadResult upload(InputStream input, long size, String originalFilename, String customFileName)
            throws IOException {
        if (input == null) {
            throw new ApiException("上传文件不能为空");
        }
        String sourceName = customFileName != null && !customFileName.isBlank()
                ? customFileName : originalFilename;
        if (sourceName == null || sourceName.isBlank()) {
            throw new ApiException("文件名不能为空");
        }
        if (size > DEFAULT_MAX_SIZE) {
            throw new ApiException("文件大小超出限制");
        }
        if (sourceName.length() > 100) {
            throw new ApiException("文件名过长");
        }

        String extension = extractExtension(sourceName);
        String baseName = extractBaseName(sourceName);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedName = baseName + "_" + System.currentTimeMillis()
                + (extension.isEmpty() ? "" : "." + extension);
        String relativePath = "upload/" + datePath + "/" + storedName;

        Path uploadRoot = profileConfig.profileRoot().resolve("upload");
        Path target = uploadRoot.resolve(datePath).resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot.normalize())) {
            throw new ApiException(403, "非法路径");
        }
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);

        String fileName = RESOURCE_PREFIX + "/" + relativePath;
        UploadResult result = new UploadResult();
        result.setFileName(fileName);
        result.setNewFileName(storedName);
        result.setOriginalFilename(sourceName);
        result.setFileExtension(extension.toLowerCase(Locale.ROOT));
        result.setUrl(fileName);
        return result;
    }

    public String uploadBase64Image(String base64, String originalFileName) throws IOException {
        if (base64 == null || base64.isBlank()) {
            throw new ApiException("无效的图片数据");
        }
        Matcher matcher = BASE64_IMAGE_PATTERN.matcher(base64);
        if (!matcher.find()) {
            throw new ApiException("无效的图片数据");
        }
        String imageFormat = matcher.group(1);
        byte[] data = Base64.getDecoder().decode(matcher.group(2));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null) {
            throw new ApiException("无效的图片数据");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedName = originalFileName + "_" + System.currentTimeMillis() + "." + imageFormat;
        Path uploadRoot = profileConfig.profileRoot().resolve("upload");
        Path target = uploadRoot.resolve(datePath).resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot.normalize())) {
            throw new ApiException(403, "非法路径");
        }
        Files.createDirectories(target.getParent());
        ImageIO.write(image, imageFormat, target.toFile());

        return RESOURCE_PREFIX + "/upload/" + datePath + "/" + storedName;
    }

    public boolean delete(String fileName) {
        if (fileName == null || fileName.isBlank() || "null".equals(fileName)) {
            return false;
        }
        String relative = fileName;
        if (relative.startsWith(RESOURCE_PREFIX)) {
            relative = relative.substring(RESOURCE_PREFIX.length());
        }
        while (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        if (relative.isBlank() || relative.contains("..")) {
            return false;
        }
        Path file = profileConfig.resolveUploadPath(relative);
        try {
            if (Files.isRegularFile(file)) {
                return Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private static String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1);
    }

    private static String extractBaseName(String filename) {
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public static class UploadResult {
        private String url;
        private String fileName;
        private String newFileName;
        private String originalFilename;
        private String fileExtension;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getNewFileName() {
            return newFileName;
        }

        public void setNewFileName(String newFileName) {
            this.newFileName = newFileName;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        public void setFileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
        }
    }
}
