package com.cat2bug.admin.resource.open;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.framework.storage.FileUploadService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

@Path("/api/file")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiFileResource {

    @Inject
    FileUploadService fileUploadService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public AjaxResult uploadFile(@RestForm("file") FileUpload file, @Context UriInfo uriInfo) {
        try {
            if (file == null || file.uploadedFile() == null) {
                return AjaxResult.error("上传文件不能为空");
            }
            long size = Files.size(file.uploadedFile());
            String originalFilename = file.fileName();
            try (var input = Files.newInputStream(file.uploadedFile())) {
                FileUploadService.UploadResult result =
                        fileUploadService.upload(input, size, originalFilename, null);
                String filePath = result.getFileName();
                String baseUrl = uriInfo.getBaseUri().toString();
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                String url = baseUrl + filePath;
                String fileExtension = result.getFileExtension();
                AjaxResult ajax = AjaxResult.success();
                ajax.put("url", url);
                ajax.put("filePath", filePath);
                ajax.put("serverFileName", extractFileName(filePath));
                ajax.put("originalFilename", originalFilename);
                ajax.put("fileExtension", StringUtils.isNotBlank(fileExtension)
                        ? fileExtension.toLowerCase(Locale.ROOT) : "");
                return ajax;
            }
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    private static String extractFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "";
        }
        int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return slash >= 0 ? filePath.substring(slash + 1) : filePath;
    }
}
