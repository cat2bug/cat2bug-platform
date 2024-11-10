package com.cat2bug.common.utils.file;

import com.cat2bug.common.exception.file.InvalidExtensionException;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-05 15:46
 * @Version: 1.0.0
 */
public interface IFileService {
    public String upload(String path, MultipartFile file) throws IOException, InvalidExtensionException;

    public void download(HttpServletResponse response, String filePath) throws IOException;

    public boolean delete(String filePath);
}