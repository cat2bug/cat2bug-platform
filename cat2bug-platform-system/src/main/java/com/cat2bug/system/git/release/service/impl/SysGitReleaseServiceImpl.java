package com.cat2bug.system.git.release.service.impl;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.DateUtils;
import com.cat2bug.common.util.MessageUtils;
import com.cat2bug.framework.security.SecurityUtils;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.common.util.FileResponseUtils;
import com.cat2bug.common.util.IdUtils;
import com.cat2bug.common.domain.SysGitRelease;
import com.cat2bug.common.domain.SysGitReleaseAsset;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.system.git.release.GitReleaseLimits;
import com.cat2bug.system.git.release.GitReleaseStoragePaths;
import com.cat2bug.system.git.release.service.ISysGitReleaseService;
import com.cat2bug.system.mapper.SysGitReleaseAssetMapper;
import com.cat2bug.system.mapper.SysGitReleaseMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.git.GitReleaseTagHelper;
import com.cat2bug.system.service.git.GitRepositoryAccessService;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.ObjectId;
import jakarta.transaction.Transactional;

import com.cat2bug.common.web.BinaryHttpResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 发行版 CRUD、附件与权限（manage 写 / pull 读，草稿仅 manage 可见）。
 */
@ApplicationScoped
public class SysGitReleaseServiceImpl implements ISysGitReleaseService
{
    @Inject
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Inject
    private SysGitReleaseMapper sysGitReleaseMapper;

    @Inject
    private SysGitReleaseAssetMapper sysGitReleaseAssetMapper;

    @Inject
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Inject
    private GitReleaseTagHelper gitReleaseTagHelper;

    @Override
    public List<SysGitRelease> listReleases(Long repositoryId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getRepositoryId());
        boolean excludeDraft = !canManage(repo.getRepositoryId());
        return sysGitReleaseMapper.selectSysGitReleaseListByRepositoryId(repositoryId, excludeDraft);
    }

    @Override
    public SysGitRelease getRelease(Long repositoryId, Long releaseId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getRepositoryId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);
        assertVisible(release, repo.getRepositoryId());
        release.setAssets(sysGitReleaseAssetMapper.selectSysGitReleaseAssetListByReleaseId(releaseId));
        fillTargetCommitMessage(repo, release);
        return release;
    }

    private void fillTargetCommitMessage(SysGitRepository repo, SysGitRelease release)
    {
        if (release == null || StringUtils.isBlank(release.getTargetCommitId()))
        {
            return;
        }
        try
        {
            String message = gitReleaseTagHelper.readCommitShortMessage(
                    repo.getDiskPath(), release.getTargetCommitId());
            if (StringUtils.isNotBlank(message))
            {
                release.setTargetCommitMessage(message);
            }
        }
        catch (IOException ignored)
        {
            // 详情页附加信息，失败不阻断主流程
        }
    }

    @Override
    @Transactional
    public SysGitRelease createRelease(Long repositoryId, SysGitRelease release)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getRepositoryId());
        validateCreateBody(release);

        String tagName = release.getTagName().trim();
        gitReleaseTagHelper.validateTagName(tagName);
        if (sysGitReleaseMapper.selectSysGitReleaseByRepositoryIdAndTagName(repositoryId, tagName) != null)
        {
            throw new ApiException(MessageUtils.message("git.release.tag-duplicate"),
                    HttpStatus.BAD_REQUEST);
        }

        ObjectId commitId;
        try
        {
            commitId = gitReleaseTagHelper.resolveTargetCommit(
                    repo.getDiskPath(),
                    release.getTargetCommitId(),
                    release.getTargetRef(),
                    repo.getDefaultBranch());
        }
        catch (IOException e)
        {
            throw new ApiException(MessageUtils.message("git.repo.browse.read-failed"),
                    HttpStatus.ERROR);
        }

        String boundCommit;
        try
        {
            boundCommit = gitReleaseTagHelper.ensureTagAtCommit(repo.getDiskPath(), tagName, commitId);
        }
        catch (IOException e)
        {
            throw new ApiException(MessageUtils.message("git.repo.browse.read-failed"),
                    HttpStatus.ERROR);
        }

        Date now = DateUtils.getNowDate();
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();

        SysGitRelease row = new SysGitRelease();
        row.setRepositoryId(repositoryId);
        row.setTagName(tagName);
        row.setTargetCommitId(boundCommit);
        row.setTitle(release.getTitle().trim());
        row.setBody(release.getBody());
        row.setDraft(booleanToInt(release.getDraft(), 0));
        row.setPrerelease(booleanToInt(release.getPrerelease(), 0));
        row.setPublisherId(userId);
        row.setPublisherName(username);
        if (!row.isDraftRelease())
        {
            row.setPublishedAt(now);
        }
        row.setCreateBy(username);
        row.setCreateTime(now);

        if (sysGitReleaseMapper.insertSysGitRelease(row) <= 0)
        {
            throw new ApiException(MessageUtils.message("git.release.create-failed"));
        }
        return getRelease(repositoryId, row.getReleaseId());
    }

    @Override
    @Transactional
    public SysGitRelease updateRelease(Long repositoryId, Long releaseId, SysGitRelease patch)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getRepositoryId());
        SysGitRelease existing = requireRelease(releaseId, repositoryId);

        if (patch.getTitle() != null)
        {
            if (StringUtils.isBlank(patch.getTitle()))
            {
                throw new ApiException(MessageUtils.message("git.release.title-required"),
                        HttpStatus.BAD_REQUEST);
            }
            existing.setTitle(patch.getTitle().trim());
        }
        if (patch.getBody() != null)
        {
            existing.setBody(patch.getBody());
        }
        if (patch.getDraft() != null)
        {
            existing.setDraft(booleanToInt(patch.getDraft(), existing.getDraft()));
        }
        if (patch.getPrerelease() != null)
        {
            existing.setPrerelease(booleanToInt(patch.getPrerelease(), existing.getPrerelease()));
        }
        if (!existing.isDraftRelease() && existing.getPublishedAt() == null)
        {
            existing.setPublishedAt(DateUtils.getNowDate());
        }
        existing.setUpdateBy(SecurityUtils.getUsername());
        existing.setUpdateTime(DateUtils.getNowDate());

        if (sysGitReleaseMapper.updateSysGitRelease(existing) <= 0)
        {
            throw new ApiException(MessageUtils.message("git.release.update-failed"));
        }
        return getRelease(repositoryId, releaseId);
    }

    @Override
    @Transactional
    public void deleteRelease(Long repositoryId, Long releaseId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getRepositoryId());
        requireRelease(releaseId, repositoryId);

        List<SysGitReleaseAsset> assets =
                sysGitReleaseAssetMapper.selectSysGitReleaseAssetListByReleaseId(releaseId);
        for (SysGitReleaseAsset asset : assets)
        {
            deleteAssetFile(asset);
        }
        sysGitReleaseAssetMapper.deleteSysGitReleaseAssetByReleaseId(releaseId);
        sysGitReleaseMapper.deleteSysGitReleaseByReleaseId(releaseId);
        // Git tag 保留（design.md）
    }

    @Override
    @Transactional
    public SysGitReleaseAsset uploadAsset(Long repositoryId, Long releaseId, java.nio.file.Path uploadedFile, String fileName, String contentType, long fileSize) throws IOException
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getRepositoryId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);

        if (uploadedFile == null || !Files.isRegularFile(uploadedFile))
        {
            throw new ApiException(MessageUtils.message("git.release.asset-empty"),
                    HttpStatus.BAD_REQUEST);
        }
        long uploadSize = fileSize > 0 ? fileSize : Files.size(uploadedFile);
        if (uploadSize > GitReleaseLimits.MAX_ASSET_FILE_BYTES)
        {
            throw new ApiException(MessageUtils.message("git.release.asset-too-large"),
                    HttpStatus.BAD_REQUEST);
        }
        int count = sysGitReleaseAssetMapper.countSysGitReleaseAssetByReleaseId(releaseId);
        if (count >= GitReleaseLimits.MAX_ASSETS_PER_RELEASE)
        {
            throw new ApiException(MessageUtils.message("git.release.asset-count-limit"),
                    HttpStatus.BAD_REQUEST);
        }

        String originalName = FilenameUtils.getName(Objects.requireNonNull(fileName));
        if (StringUtils.isBlank(originalName))
        {
            throw new ApiException(MessageUtils.message("git.release.asset-name-invalid"),
                    HttpStatus.BAD_REQUEST);
        }

        String storedName = IdUtils.fastSimpleUUID() + "_" + originalName;
        File dir = GitReleaseStoragePaths.releaseAssetAbsoluteDir(repositoryId, releaseId);
        if (!dir.exists() && !dir.mkdirs())
        {
            throw new ApiException(MessageUtils.message("git.release.asset-save-failed"));
        }
        File dest = new File(dir, storedName);
        Files.copy(uploadedFile, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

        String storagePath = GitReleaseStoragePaths.storagePath(repositoryId, releaseId, storedName);
        Date now = DateUtils.getNowDate();
        String username = SecurityUtils.getUsername();

        SysGitReleaseAsset asset = new SysGitReleaseAsset();
        asset.setReleaseId(releaseId);
        asset.setRepositoryId(repositoryId);
        asset.setFileName(originalName);
        asset.setContentType(StringUtils.isNotBlank(contentType) ? contentType
                : "application/octet-stream");
        asset.setFileSize(uploadSize);
        asset.setStoragePath(storagePath);
        asset.setCreateBy(username);
        asset.setCreateTime(now);

        if (sysGitReleaseAssetMapper.insertSysGitReleaseAsset(asset) <= 0)
        {
            FileResponseUtils.deleteFile(dest.getAbsolutePath());
            throw new ApiException(MessageUtils.message("git.release.asset-save-failed"));
        }
        return asset;
    }

    @Override
    public List<SysGitReleaseAsset> listAssets(Long repositoryId, Long releaseId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getRepositoryId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);
        assertVisible(release, repo.getRepositoryId());
        return sysGitReleaseAssetMapper.selectSysGitReleaseAssetListByReleaseId(releaseId);
    }

    @Override
    @Transactional
    public void deleteAsset(Long repositoryId, Long releaseId, Long assetId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getRepositoryId());
        requireRelease(releaseId, repositoryId);
        SysGitReleaseAsset asset = requireAsset(assetId, releaseId, repositoryId);
        deleteAssetFile(asset);
        sysGitReleaseAssetMapper.deleteSysGitReleaseAssetByAssetId(assetId);
    }

    @Override
    public void downloadAsset(Long repositoryId, Long releaseId, Long assetId, BinaryHttpResponse response)
            throws IOException
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getRepositoryId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);
        assertVisible(release, repo.getRepositoryId());
        SysGitReleaseAsset asset = requireAsset(assetId, releaseId, repositoryId);

        File file = GitReleaseStoragePaths.absoluteFile(asset.getStoragePath());
        if (!file.isFile())
        {
            throw new ApiException(MessageUtils.message("git.release.asset-not-found"),
                    HttpStatus.NOT_FOUND);
        }
        String contentType = asset.getContentType();
        response.setContentType(StringUtils.isNotBlank(contentType) ? contentType : "application/octet-stream");
        FileResponseUtils.setAttachmentResponseHeader(response, asset.getFileName());
        FileResponseUtils.writeBytes(file.getAbsolutePath(), response.getOutputStream());
    }

    // ------------------------------------------------------------------

    private void validateCreateBody(SysGitRelease release)
    {
        if (release == null || StringUtils.isBlank(release.getTagName()))
        {
            throw new ApiException(MessageUtils.message("git.release.tag-required"),
                    HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isBlank(release.getTitle()))
        {
            throw new ApiException(MessageUtils.message("git.release.title-required"),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private SysGitRepository requireRepository(Long repositoryId)
    {
        SysGitRepository repo = repositoryId == null
                ? null : sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null)
        {
            throw new ApiException(MessageUtils.message("git.repo.not-found"),
                    HttpStatus.NOT_FOUND);
        }
        return repo;
    }

    private SysGitRelease requireRelease(Long releaseId, Long repositoryId)
    {
        SysGitRelease release = releaseId == null ? null : sysGitReleaseMapper.selectSysGitReleaseByReleaseId(releaseId);
        if (release == null || !Objects.equals(release.getRepositoryId(), repositoryId))
        {
            throw new ApiException(MessageUtils.message("git.release.not-found"),
                    HttpStatus.NOT_FOUND);
        }
        return release;
    }

    private SysGitReleaseAsset requireAsset(Long assetId, Long releaseId, Long repositoryId)
    {
        SysGitReleaseAsset asset = assetId == null ? null : sysGitReleaseAssetMapper.selectSysGitReleaseAssetByAssetId(assetId);
        if (asset == null || !Objects.equals(asset.getReleaseId(), releaseId)
                || !Objects.equals(asset.getRepositoryId(), repositoryId))
        {
            throw new ApiException(MessageUtils.message("git.release.asset-not-found"),
                    HttpStatus.NOT_FOUND);
        }
        return asset;
    }

    private void assertVisible(SysGitRelease release, Long repositoryId)
    {
        if (release.isDraftRelease() && !canManage(repositoryId))
        {
            throw new ApiException(MessageUtils.message("no-permission"),
                    HttpStatus.FORBIDDEN);
        }
    }

    private void assertPull(Long repositoryId)
    {
        if (!gitRepositoryAccessService.canPull(SecurityUtils.getUserId(), repositoryId))
        {
            throw new ApiException(MessageUtils.message("no-permission"),
                    HttpStatus.FORBIDDEN);
        }
    }

    private void assertManage(Long repositoryId)
    {
        if (!canManage(repositoryId))
        {
            throw new ApiException(MessageUtils.message("no-permission"),
                    HttpStatus.FORBIDDEN);
        }
    }

    private boolean canManage(Long repositoryId)
    {
        return gitRepositoryAccessService.canManage(SecurityUtils.getUserId(), repositoryId);
    }

    private static int booleanToInt(Integer value, Integer defaultValue)
    {
        if (value == null)
        {
            return defaultValue != null ? defaultValue : 0;
        }
        return value != 0 ? 1 : 0;
    }

    private static void deleteAssetFile(SysGitReleaseAsset asset)
    {
        if (asset == null || StringUtils.isBlank(asset.getStoragePath()))
        {
            return;
        }
        File file = GitReleaseStoragePaths.absoluteFile(asset.getStoragePath());
        if (file.isFile())
        {
            FileResponseUtils.deleteFile(file.getAbsolutePath());
        }
    }
}
