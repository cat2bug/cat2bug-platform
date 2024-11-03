package com.cat2bug.api.controller;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.file.FileUploadUtils;
import com.cat2bug.common.utils.file.FileUtils;
import com.cat2bug.framework.config.ServerConfig;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.cat2bug.common.core.domain.AjaxResult.success;

/**
 * 通用请求处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/api/file/")
public class FileController
{
    @Autowired
    private ServerConfig serverConfig;

    private static final String FILE_DELIMETER = ",";

    /**
     * 通用上传请求（单个）
     */
    @PreAuthorize("@ss.hasPermi('api:defect:list')")
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = Cat2BugConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file, null);
            // 获取文件扩展名
            String fileExtension = FilenameUtils.getExtension(fileName);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = success();
            ajax.put("url", url);
            ajax.put("filePath", fileName);
            ajax.put("serverFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("fileExtension", StringUtils.isNotBlank(fileExtension)?fileExtension.toLowerCase():"");
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }
}
