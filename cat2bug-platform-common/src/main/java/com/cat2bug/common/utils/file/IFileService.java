package com.cat2bug.common.utils.file;

import com.cat2bug.common.exception.file.InvalidExtensionException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-05 15:46
 * @Version: 1.0.0
 */
public interface IFileService {
    public String upload(String path, MultipartFile file, boolean isProjectFile) throws IOException, InvalidExtensionException;

    public String uploadLocal(String path) throws IOException, InvalidExtensionException;

    public String uploadPackagePart(PackagePart part, boolean isProjectFile) throws IOException;

    public String uploadImportBytes(byte[] data, boolean isProjectFile) throws IOException;

    public void download(HttpServletResponse response, String filePath) throws IOException;

    public boolean delete(String filePath);

    public String downloadToLocal(String filePath) throws IOException;
}