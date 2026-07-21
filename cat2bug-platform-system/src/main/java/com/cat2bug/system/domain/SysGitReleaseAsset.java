package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.BaseEntity;

/**
 * Git 发行版附件 {@code sys_git_release_asset}
 */
public class SysGitReleaseAsset extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long assetId;

    private Long releaseId;

    private Long repositoryId;

    private String fileName;

    private String contentType;

    private Long fileSize;

    /** 相对 {@code cat2bug.profile} 的路径 */
    private String storagePath;

    public Long getAssetId()
    {
        return assetId;
    }

    public void setAssetId(Long assetId)
    {
        this.assetId = assetId;
    }

    public Long getReleaseId()
    {
        return releaseId;
    }

    public void setReleaseId(Long releaseId)
    {
        this.releaseId = releaseId;
    }

    public Long getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public Long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize(Long fileSize)
    {
        this.fileSize = fileSize;
    }

    public String getStoragePath()
    {
        return storagePath;
    }

    public void setStoragePath(String storagePath)
    {
        this.storagePath = storagePath;
    }
}
