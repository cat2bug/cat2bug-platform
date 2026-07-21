package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.SysGitRelease;
import com.cat2bug.system.git.release.service.ISysGitReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Git 发行版 REST：{@code /system/git/repo/{repositoryId}/releases/**}。
 */
@RestController
public class SysGitReleaseController extends BaseController
{
    @Autowired
    private ISysGitReleaseService sysGitReleaseService;

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/releases")
    public AjaxResult list(@PathVariable Long repositoryId)
    {
        return success(sysGitReleaseService.listReleases(repositoryId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/releases/{releaseId}")
    public AjaxResult get(
            @PathVariable Long repositoryId,
            @PathVariable Long releaseId)
    {
        return success(sysGitReleaseService.getRelease(repositoryId, releaseId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git发行版", businessType = BusinessType.INSERT)
    @PostMapping("/system/git/repo/{repositoryId}/releases")
    public AjaxResult create(
            @PathVariable Long repositoryId,
            @RequestBody SysGitRelease body)
    {
        return success(sysGitReleaseService.createRelease(repositoryId, body));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git发行版", businessType = BusinessType.UPDATE)
    @PutMapping("/system/git/repo/{repositoryId}/releases/{releaseId}")
    public AjaxResult update(
            @PathVariable Long repositoryId,
            @PathVariable Long releaseId,
            @RequestBody SysGitRelease body)
    {
        return success(sysGitReleaseService.updateRelease(repositoryId, releaseId, body));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git发行版", businessType = BusinessType.DELETE)
    @DeleteMapping("/system/git/repo/{repositoryId}/releases/{releaseId}")
    public AjaxResult remove(
            @PathVariable Long repositoryId,
            @PathVariable Long releaseId)
    {
        sysGitReleaseService.deleteRelease(repositoryId, releaseId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git发行版附件", businessType = BusinessType.INSERT)
    @PostMapping("/system/git/repo/{repositoryId}/releases/{releaseId}/assets")
    public AjaxResult uploadAsset(
            @PathVariable Long repositoryId,
            @PathVariable Long releaseId,
            @RequestParam("file") MultipartFile file) throws IOException
    {
        return success(sysGitReleaseService.uploadAsset(repositoryId, releaseId, file));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/releases/{releaseId}/assets")
    public AjaxResult listAssets(
            @PathVariable Long repositoryId,
            @PathVariable Long releaseId)
    {
        return success(sysGitReleaseService.listAssets(repositoryId, releaseId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git发行版附件", businessType = BusinessType.DELETE)
    @DeleteMapping("/system/git/repo/{repositoryId}/releases/{releaseId}/assets/{assetId}")
    public AjaxResult deleteAsset(
            @PathVariable Long repositoryId,
            @PathVariable Long releaseId,
            @PathVariable Long assetId)
    {
        sysGitReleaseService.deleteAsset(repositoryId, releaseId, assetId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/releases/{releaseId}/assets/{assetId}/download")
    public void downloadAsset(
            @PathVariable Long repositoryId,
            @PathVariable Long releaseId,
            @PathVariable Long assetId,
            HttpServletResponse response) throws IOException
    {
        sysGitReleaseService.downloadAsset(repositoryId, releaseId, assetId, response);
    }
}
