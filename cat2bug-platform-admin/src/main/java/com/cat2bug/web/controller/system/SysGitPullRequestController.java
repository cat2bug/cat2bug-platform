package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.SysGitPullRequest;
import com.cat2bug.system.domain.SysGitPullRequestComment;
import com.cat2bug.system.git.pull.service.ISysGitPullRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Git Pull Request REST：{@code /system/git/repo/{repositoryId}/pulls/**}。
 */
@RestController
public class SysGitPullRequestController extends BaseController
{
    @Autowired
    private ISysGitPullRequestService sysGitPullRequestService;

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/pulls")
    public AjaxResult list(@PathVariable Long repositoryId, @RequestParam(required = false) String state)
    {
        return success(sysGitPullRequestService.listPullRequests(repositoryId, state));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:push')")
    @Log(title = "Git Pull Request", businessType = BusinessType.INSERT)
    @PostMapping("/system/git/repo/{repositoryId}/pulls")
    public AjaxResult create(@PathVariable Long repositoryId, @RequestBody SysGitPullRequest body)
    {
        return success(sysGitPullRequestService.createPullRequest(repositoryId, body));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/pulls/{number}")
    public AjaxResult get(@PathVariable Long repositoryId, @PathVariable Integer number)
    {
        return success(sysGitPullRequestService.getPullRequest(repositoryId, number));
    }

    @Log(title = "Git Pull Request", businessType = BusinessType.UPDATE)
    @PatchMapping("/system/git/repo/{repositoryId}/pulls/{number}")
    public AjaxResult close(@PathVariable Long repositoryId, @PathVariable Integer number)
    {
        sysGitPullRequestService.closePullRequest(repositoryId, number);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/pulls/{number}/commits")
    public AjaxResult commits(@PathVariable Long repositoryId, @PathVariable Integer number)
    {
        return success(sysGitPullRequestService.listCommits(repositoryId, number));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/pulls/{number}/files")
    public AjaxResult files(@PathVariable Long repositoryId, @PathVariable Integer number)
    {
        return success(sysGitPullRequestService.listFiles(repositoryId, number));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/system/git/repo/{repositoryId}/pulls/{number}/comments")
    public AjaxResult comments(@PathVariable Long repositoryId, @PathVariable Integer number)
    {
        return success(sysGitPullRequestService.listComments(repositoryId, number));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:push')")
    @Log(title = "Git Pull Request 评论", businessType = BusinessType.INSERT)
    @PostMapping("/system/git/repo/{repositoryId}/pulls/{number}/comments")
    public AjaxResult createComment(
            @PathVariable Long repositoryId, @PathVariable Integer number, @RequestBody SysGitPullRequestComment body)
    {
        return success(sysGitPullRequestService.createComment(repositoryId, number, body));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "Git Pull Request", businessType = BusinessType.UPDATE)
    @PostMapping("/system/git/repo/{repositoryId}/pulls/{number}/merge")
    public AjaxResult merge(@PathVariable Long repositoryId, @PathVariable Integer number)
    {
        return success(sysGitPullRequestService.mergePullRequest(repositoryId, number));
    }
}
