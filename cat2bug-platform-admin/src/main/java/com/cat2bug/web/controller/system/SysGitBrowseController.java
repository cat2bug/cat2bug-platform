package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.git.browse.domain.GitBranchCreateRequest;
import com.cat2bug.system.git.browse.domain.GitBranchNameRequest;
import com.cat2bug.system.git.browse.domain.GitTagCreateRequest;
import com.cat2bug.system.git.browse.service.ISysGitBrowseService;
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

/**
 * 项目 Git 代码仓库浏览 Controller：分支 / 目录 / 文件内容 / 提交记录，均要求 pull 权限。
 *
 * <p>OpenSpec {@code project-git-repositories} 任务 4.1。仓库元数据的增删改查
 * （{@code GET/POST/PUT/DELETE /system/git/repo}，含列表与详情）由 {@code SysGitRepositoryController}
 * （任务 2.x）负责；Smart HTTP clone/push（任务 3.x）另行挂载。本 Controller 只读，专注仓库内容浏览，
 * 不与前者的路径重叠。</p>
 */
@RestController
public class SysGitBrowseController extends BaseController
{
    @Autowired
    private ISysGitBrowseService gitBrowseService;

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/branches")
    public AjaxResult branches(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String name)
    {
        return success(gitBrowseService.listBranches(repositoryId, name));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git分支", businessType = BusinessType.INSERT)
    @PostMapping("/system/git/repo/{repositoryId}/branches")
    public AjaxResult createBranch(
            @PathVariable Long repositoryId,
            @RequestBody GitBranchCreateRequest body)
    {
        return success(gitBrowseService.createBranch(repositoryId, body.getName(), body.getFromRef()));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git分支", businessType = BusinessType.DELETE)
    @DeleteMapping("/system/git/repo/{repositoryId}/branches/{name:.+}")
    public AjaxResult deleteBranch(
            @PathVariable Long repositoryId,
            @PathVariable String name)
    {
        gitBrowseService.deleteBranch(repositoryId, name);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git默认分支", businessType = BusinessType.UPDATE)
    @PutMapping("/system/git/repo/{repositoryId}/default-branch")
    public AjaxResult setDefaultBranch(
            @PathVariable Long repositoryId,
            @RequestBody GitBranchNameRequest body)
    {
        gitBrowseService.setDefaultBranch(repositoryId, body.getName());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/tags")
    public AjaxResult tags(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String q)
    {
        return success(gitBrowseService.listTags(repositoryId, q));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git标签", businessType = BusinessType.INSERT)
    @PostMapping("/system/git/repo/{repositoryId}/tags")
    public AjaxResult createTag(
            @PathVariable Long repositoryId,
            @RequestBody GitTagCreateRequest body)
    {
        return success(gitBrowseService.createTag(
                repositoryId,
                body.getName(),
                body.getMessage(),
                body.getTargetRef(),
                body.getTargetCommitId()));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/system/git/repo/{repositoryId}/tags/{name:.+}")
    public AjaxResult deleteTag(
            @PathVariable Long repositoryId,
            @PathVariable String name,
            @RequestParam(required = false) Boolean cascadeRelease)
    {
        gitBrowseService.deleteTag(repositoryId, name, cascadeRelease);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/tree")
    public AjaxResult tree(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) String path)
    {
        return success(gitBrowseService.listTree(repositoryId, ref, path));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/blob")
    public AjaxResult blob(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String ref,
            @RequestParam String path)
    {
        return success(gitBrowseService.readBlob(repositoryId, ref, path));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/raw")
    public void raw(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String ref,
            @RequestParam String path,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException
    {
        gitBrowseService.writeRawBlob(repositoryId, ref, path, response);
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/commit-count")
    public AjaxResult commitCount(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String ref)
    {
        return success(gitBrowseService.countCommits(repositoryId, ref));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/commits/{commitId}")
    public AjaxResult commitDetail(
            @PathVariable Long repositoryId,
            @PathVariable String commitId)
    {
        return success(gitBrowseService.getCommitDetail(repositoryId, commitId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/commits")
    public AjaxResult commits(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until)
    {
        return success(gitBrowseService.listCommits(repositoryId, ref, limit, author, since, until));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/contributors")
    public AjaxResult contributors(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String ref,
            @RequestParam(required = false) Integer limit)
    {
        return success(gitBrowseService.listContributors(repositoryId, ref, limit));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/archive")
    public void archive(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String ref,
            @RequestParam(defaultValue = "zip") String format,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException
    {
        gitBrowseService.writeArchive(repositoryId, ref, format, response);
    }
}
