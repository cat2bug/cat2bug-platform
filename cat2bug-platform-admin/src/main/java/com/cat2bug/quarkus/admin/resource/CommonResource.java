package com.cat2bug.quarkus.admin.resource;

import com.cat2bug.quarkus.common.api.AjaxResult;
import com.cat2bug.quarkus.common.domain.SysTempFile;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.framework.storage.FileDownloadHelper;
import com.cat2bug.quarkus.framework.storage.FileUploadService;
import com.cat2bug.quarkus.system.service.SysTempFileService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/common")
@Produces(MediaType.APPLICATION_JSON)
public class CommonResource {

    @Inject
    FileUploadService fileUploadService;

    @Inject
    SysTempFileService sysTempFileService;

    @Inject
    FileDownloadHelper fileDownloadHelper;

    @GET
    @Path("/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@QueryParam("fileName") String fileName,
                             @QueryParam("delete") Boolean delete) throws IOException {
        return fileDownloadHelper.downloadByFileName(fileName, Boolean.TRUE.equals(delete));
    }

    @GET
    @Path("/download/resource")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response resourceDownload(@QueryParam("resource") String resource) throws IOException {
        return fileDownloadHelper.downloadResource(resource);
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public AjaxResult upload(@RestForm("file") FileUpload file,
                             @RestForm("customFileName") String customFileName) throws IOException {
        if (file == null || file.uploadedFile() == null) {
            throw new ApiException("上传文件不能为空");
        }
        long size = Files.size(file.uploadedFile());
        String originalFilename = file.fileName();
        try (var input = Files.newInputStream(file.uploadedFile())) {
            FileUploadService.UploadResult result =
                    fileUploadService.upload(input, size, originalFilename, customFileName);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", result.getUrl());
            ajax.put("fileName", result.getFileName());
            ajax.put("newFileName", result.getNewFileName());
            ajax.put("originalFilename", result.getOriginalFilename());
            ajax.put("fileExtension", result.getFileExtension());
            return ajax;
        }
    }

    @POST
    @Path("/uploads")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public AjaxResult uploads(@RestForm("files") List<FileUpload> files) {
        if (files == null || files.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }
        List<String> urls = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        List<String> newFileNames = new ArrayList<>();
        List<String> originalFilenames = new ArrayList<>();
        try {
            for (FileUpload file : files) {
                if (file == null || file.uploadedFile() == null) {
                    continue;
                }
                try (var input = Files.newInputStream(file.uploadedFile())) {
                    FileUploadService.UploadResult result = fileUploadService.upload(
                            input, Files.size(file.uploadedFile()), file.fileName(), null);
                    urls.add(result.getUrl());
                    fileNames.add(result.getFileName());
                    newFileNames.add(result.getNewFileName());
                    originalFilenames.add(result.getOriginalFilename());
                }
            }
        } catch (IOException | RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("urls", String.join(",", urls));
        ajax.put("fileNames", String.join(",", fileNames));
        ajax.put("newFileNames", String.join(",", newFileNames));
        ajax.put("originalFilenames", String.join(",", originalFilenames));
        return ajax;
    }

    @POST
    @Path("/upload/screen-shot")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult uploadScreenShot(SysTempFile file) throws IOException {
        String base64Code = String.valueOf(file.getFileBody());
        String originalFileName = "screen_" + UUID.randomUUID().toString().substring(0, 8);
        String path = fileUploadService.uploadBase64Image(base64Code, originalFileName);
        file.setFileName(originalFileName);
        file.setFileUrl(path);
        file.setFileType(SysTempFileService.FILE_TYPE_IMAGE);
        sysTempFileService.insertSysTempFile(file);
        file.setFileBody(null);
        return AjaxResult.success(file);
    }

    @DELETE
    @Path("/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    public AjaxResult delete(Map<String, Object> body) {
        String fileName = String.valueOf(body.get("fileName"));
        if (fileUploadService.delete(fileName)) {
            return AjaxResult.success();
        }
        return AjaxResult.error("操作失败");
    }
}
