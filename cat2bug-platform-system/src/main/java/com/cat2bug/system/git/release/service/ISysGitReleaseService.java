package com.cat2bug.system.git.release.service;

import com.cat2bug.system.domain.SysGitRelease;
import com.cat2bug.system.domain.SysGitReleaseAsset;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Git 发行版与附件服务。
 */
public interface ISysGitReleaseService
{
    List<SysGitRelease> listReleases(Long repositoryId);

    SysGitRelease getRelease(Long repositoryId, Long releaseId);

    SysGitRelease createRelease(Long repositoryId, SysGitRelease release);

    SysGitRelease updateRelease(Long repositoryId, Long releaseId, SysGitRelease release);

    void deleteRelease(Long repositoryId, Long releaseId);

    SysGitReleaseAsset uploadAsset(Long repositoryId, Long releaseId, MultipartFile file) throws IOException;

    List<SysGitReleaseAsset> listAssets(Long repositoryId, Long releaseId);

    void deleteAsset(Long repositoryId, Long releaseId, Long assetId);

    void downloadAsset(Long repositoryId, Long releaseId, Long assetId, HttpServletResponse response)
            throws IOException;
}
