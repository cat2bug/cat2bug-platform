package com.cat2bug.web.vo;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-08-07 00:51
 * @Version: 1.0.0
 * 自定义文件类
 */
public class CustomMultipartFile implements MultipartFile {
    private final MultipartFile originalFile;
    private final String newFilename;

    public CustomMultipartFile(MultipartFile originalFile, String newFilename) {
        this.originalFile = originalFile;
        this.newFilename = newFilename;
    }

    @Override
    public String getName() {
        return originalFile.getName();
    }

    @Override
    public String getOriginalFilename() {
        return newFilename; // 返回自定义文件名
    }

    @Override
    public String getContentType() {
        return originalFile.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return originalFile.isEmpty();
    }

    @Override
    public long getSize() {
        return originalFile.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return originalFile.getBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return originalFile.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        originalFile.transferTo(dest);
    }
}
