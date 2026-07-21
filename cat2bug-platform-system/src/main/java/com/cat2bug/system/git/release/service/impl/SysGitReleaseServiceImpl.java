package com.cat2bug.system.git.release.service.impl;

import com.cat2bug.common.constant.HttpStatus;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.DateUtils;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.file.FileUtils;
import com.cat2bug.common.utils.uuid.IdUtils;
import com.cat2bug.system.domain.SysGitRelease;
import com.cat2bug.system.domain.SysGitReleaseAsset;
import com.cat2bug.system.domain.SysGitRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
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
@Service
public class SysGitReleaseServiceImpl implements ISysGitReleaseService
{
    @Autowired
    private SysGitRepositoryMapper sysGitRepositoryMapper;

    @Autowired
    private SysGitReleaseMapper sysGitReleaseMapper;

    @Autowired
    private SysGitReleaseAssetMapper sysGitReleaseAssetMapper;

    @Autowired
    private GitRepositoryAccessService gitRepositoryAccessService;

    @Autowired
    private GitReleaseTagHelper gitReleaseTagHelper;

    @Override
    public List<SysGitRelease> listReleases(Long repositoryId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getProjectId());
        boolean excludeDraft = !canManage(repo.getProjectId());
        return sysGitReleaseMapper.selectSysGitReleaseListByRepositoryId(repositoryId, excludeDraft);
    }

    @Override
    public SysGitRelease getRelease(Long repositoryId, Long releaseId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getProjectId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);
        assertVisible(release, repo.getProjectId());
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
        assertManage(repo.getProjectId());
        validateCreateBody(release);

        String tagName = release.getTagName().trim();
        gitReleaseTagHelper.validateTagName(tagName);
        if (sysGitReleaseMapper.selectSysGitReleaseByRepositoryIdAndTagName(repositoryId, tagName) != null)
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.tag-duplicate"),
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
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.repo.browse.read-failed"),
                    HttpStatus.ERROR);
        }

        String boundCommit;
        try
        {
            boundCommit = gitReleaseTagHelper.ensureTagAtCommit(repo.getDiskPath(), tagName, commitId);
        }
        catch (IOException e)
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.repo.browse.read-failed"),
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
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.create-failed"));
        }
        return getRelease(repositoryId, row.getReleaseId());
    }

    @Override
    @Transactional
    public SysGitRelease updateRelease(Long repositoryId, Long releaseId, SysGitRelease patch)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getProjectId());
        SysGitRelease existing = requireRelease(releaseId, repositoryId);

        if (patch.getTitle() != null)
        {
            if (StringUtils.isBlank(patch.getTitle()))
            {
                throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.title-required"),
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
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.update-failed"));
        }
        return getRelease(repositoryId, releaseId);
    }

    @Override
    @Transactional
    public void deleteRelease(Long repositoryId, Long releaseId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getProjectId());
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
    public SysGitReleaseAsset uploadAsset(Long repositoryId, Long releaseId, MultipartFile file) throws IOException
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getProjectId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);

        if (file == null || file.isEmpty())
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-empty"),
                    HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > GitReleaseLimits.MAX_ASSET_FILE_BYTES)
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-too-large"),
                    HttpStatus.BAD_REQUEST);
        }
        int count = sysGitReleaseAssetMapper.countSysGitReleaseAssetByReleaseId(releaseId);
        if (count >= GitReleaseLimits.MAX_ASSETS_PER_RELEASE)
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-count-limit"),
                    HttpStatus.BAD_REQUEST);
        }

        String originalName = FilenameUtils.getName(Objects.requireNonNull(file.getOriginalFilename()));
        if (StringUtils.isBlank(originalName))
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-name-invalid"),
                    HttpStatus.BAD_REQUEST);
        }

        String storedName = IdUtils.fastSimpleUUID() + "_" + originalName;
        File dir = GitReleaseStoragePaths.releaseAssetAbsoluteDir(repositoryId, releaseId);
        if (!dir.exists() && !dir.mkdirs())
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-save-failed"));
        }
        File dest = new File(dir, storedName);
        Files.copy(file.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

        String storagePath = GitReleaseStoragePaths.storagePath(repositoryId, releaseId, storedName);
        Date now = DateUtils.getNowDate();
        String username = SecurityUtils.getUsername();

        SysGitReleaseAsset asset = new SysGitReleaseAsset();
        asset.setReleaseId(releaseId);
        asset.setRepositoryId(repositoryId);
        asset.setFileName(originalName);
        asset.setContentType(StringUtils.isNotBlank(file.getContentType()) ? file.getContentType()
                : "application/octet-stream");
        asset.setFileSize(file.getSize());
        asset.setStoragePath(storagePath);
        asset.setCreateBy(username);
        asset.setCreateTime(now);

        if (sysGitReleaseAssetMapper.insertSysGitReleaseAsset(asset) <= 0)
        {
            FileUtils.deleteFile(dest.getAbsolutePath());
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-save-failed"));
        }
        return asset;
    }

    @Override
    public List<SysGitReleaseAsset> listAssets(Long repositoryId, Long releaseId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getProjectId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);
        assertVisible(release, repo.getProjectId());
        return sysGitReleaseAssetMapper.selectSysGitReleaseAssetListByReleaseId(releaseId);
    }

    @Override
    @Transactional
    public void deleteAsset(Long repositoryId, Long releaseId, Long assetId)
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertManage(repo.getProjectId());
        requireRelease(releaseId, repositoryId);
        SysGitReleaseAsset asset = requireAsset(assetId, releaseId, repositoryId);
        deleteAssetFile(asset);
        sysGitReleaseAssetMapper.deleteSysGitReleaseAssetByAssetId(assetId);
    }

    @Override
    public void downloadAsset(Long repositoryId, Long releaseId, Long assetId, HttpServletResponse response)
            throws IOException
    {
        SysGitRepository repo = requireRepository(repositoryId);
        assertPull(repo.getProjectId());
        SysGitRelease release = requireRelease(releaseId, repositoryId);
        assertVisible(release, repo.getProjectId());
        SysGitReleaseAsset asset = requireAsset(assetId, releaseId, repositoryId);

        File file = GitReleaseStoragePaths.absoluteFile(asset.getStoragePath());
        if (!file.isFile())
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-not-found"),
                    HttpStatus.NOT_FOUND);
        }
        String contentType = asset.getContentType();
        response.setContentType(StringUtils.isNotBlank(contentType) ? contentType : "application/octet-stream");
        FileUtils.setAttachmentResponseHeader(response, asset.getFileName());
        FileUtils.writeBytes(file.getAbsolutePath(), response.getOutputStream());
    }

    // ------------------------------------------------------------------

    private void validateCreateBody(SysGitRelease release)
    {
        if (release == null || StringUtils.isBlank(release.getTagName()))
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.tag-required"),
                    HttpStatus.BAD_REQUEST);
        }
        if (StringUtils.isBlank(release.getTitle()))
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.title-required"),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private SysGitRepository requireRepository(Long repositoryId)
    {
        SysGitRepository repo = repositoryId == null
                ? null : sysGitRepositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId);
        if (repo == null)
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.repo.not-found"),
                    HttpStatus.NOT_FOUND);
        }
        return repo;
    }

    private SysGitRelease requireRelease(Long releaseId, Long repositoryId)
    {
        SysGitRelease release = releaseId == null ? null : sysGitReleaseMapper.selectSysGitReleaseByReleaseId(releaseId);
        if (release == null || !Objects.equals(release.getRepositoryId(), repositoryId))
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.not-found"),
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
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("git.release.asset-not-found"),
                    HttpStatus.NOT_FOUND);
        }
        return asset;
    }

    private void assertVisible(SysGitRelease release, Long projectId)
    {
        if (release.isDraftRelease() && !canManage(projectId))
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("no-permission"),
                    HttpStatus.FORBIDDEN);
        }
    }

    private void assertPull(Long projectId)
    {
        if (!gitRepositoryAccessService.canPull(SecurityUtils.getUserId(), projectId))
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("no-permission"),
                    HttpStatus.FORBIDDEN);
        }
    }

    private void assertManage(Long projectId)
    {
        if (!canManage(projectId))
        {
            throw new ServiceException(com.cat2bug.common.utils.MessageUtils.message("no-permission"),
                    HttpStatus.FORBIDDEN);
        }
    }

    private boolean canManage(Long projectId)
    {
        return gitRepositoryAccessService.canManage(SecurityUtils.getUserId(), projectId);
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
            FileUtils.deleteFile(file.getAbsolutePath());
        }
    }
}
