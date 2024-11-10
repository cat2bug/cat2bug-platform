package com.cat2bug.common.utils.file.impl;

import com.cat2bug.common.exception.file.InvalidExtensionException;
import com.cat2bug.common.utils.file.FileUploadUtils;
import com.cat2bug.common.utils.file.FileUtils;
import com.cat2bug.common.utils.file.IFileService;
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
    public String upload(String path, MultipartFile file) throws IOException, InvalidExtensionException {
        return FileUploadUtils.upload(path, file, null);
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
