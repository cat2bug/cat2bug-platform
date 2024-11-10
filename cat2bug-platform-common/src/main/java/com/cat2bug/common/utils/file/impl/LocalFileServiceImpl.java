package com.cat2bug.common.utils.file.impl;

import com.cat2bug.common.exception.file.InvalidExtensionException;
import com.cat2bug.common.utils.file.FileUploadUtils;
import com.cat2bug.common.utils.file.FileUtils;
import com.cat2bug.common.utils.file.IFileService;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-10 09:52
 * @Version: 1.0.0
 */
@Service
public class LocalFileServiceImpl implements IFileService {
    @Override
    public String upload(String path, MultipartFile file, boolean isProjectFile) throws IOException, InvalidExtensionException {
        return FileUploadUtils.upload(path, file, null);
    }

    @Override
    public String uploadPackagePart(PackagePart part, boolean isProjectFile) throws IOException {
        return FileUtils.writePackagePart(part);
    }

    @Override
    public String uploadImportBytes(byte[] data, boolean isProjectFile) throws IOException {
        return FileUtils.writeImportBytes(data);
    }

    @Override
    public void download(HttpServletResponse response, String filePath) throws IOException {
        FileUtils.writeBytes(filePath, response.getOutputStream());
    }

    @Override
    public boolean delete(String filePath) {
        return FileUtils.deleteFile(filePath);
    }
}
