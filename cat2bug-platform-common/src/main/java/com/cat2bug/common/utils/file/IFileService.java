package com.cat2bug.common.utils.file;

import com.cat2bug.common.exception.file.InvalidExtensionException;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-05 15:46
 * @Version: 1.0.0
 */
public interface IFileService {
    public String upload(String path, MultipartFile file, boolean isProjectFile) throws IOException, InvalidExtensionException;

    public String uploadLocal(String path, String filePath) throws IOException, InvalidExtensionException;

    public String uploadImportBytes(byte[] data, boolean isProjectFile) throws IOException;

    public void download(HttpServletResponse response, String filePath) throws IOException;

    public boolean delete(String filePath);

    public String downloadToLocal(String filePath) throws IOException;
}