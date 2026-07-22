package com.cat2bug.web.controller.system;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.system.domain.git.SysGitRepositoryWebhookRequest;
import com.cat2bug.system.service.ISysGitRepositoryWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓库 Webhook 管理 API（effective manage 由服务层 {@code GitRepositoryAccessService} 判定）。
 */
@RestController
@RequestMapping("/system/git/repo/{repositoryId}/webhooks")
public class SysGitWebhookController extends BaseController
{
    @Autowired
    private ISysGitRepositoryWebhookService webhookService;

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping
    public AjaxResult list(@PathVariable Long repositoryId)
    {
        return success(webhookService.listWebhooks(repositoryId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/{webhookId}")
    public AjaxResult get(@PathVariable Long repositoryId, @PathVariable Long webhookId)
    {
        return success(webhookService.getWebhook(repositoryId, webhookId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库 Webhook", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@PathVariable Long repositoryId, @RequestBody SysGitRepositoryWebhookRequest body)
    {
        return success(webhookService.createWebhook(repositoryId, body));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库 Webhook", businessType = BusinessType.UPDATE)
    @PutMapping("/{webhookId}")
    public AjaxResult update(@PathVariable Long repositoryId, @PathVariable Long webhookId,
            @RequestBody SysGitRepositoryWebhookRequest body)
    {
        return success(webhookService.updateWebhook(repositoryId, webhookId, body));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库 Webhook", businessType = BusinessType.DELETE)
    @DeleteMapping("/{webhookId}")
    public AjaxResult delete(@PathVariable Long repositoryId, @PathVariable Long webhookId)
    {
        return toAjax(webhookService.deleteWebhook(repositoryId, webhookId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库 Webhook Secret", businessType = BusinessType.UPDATE)
    @PostMapping("/{webhookId}/secret/regenerate")
    public AjaxResult regenerateSecret(@PathVariable Long repositoryId, @PathVariable Long webhookId)
    {
        return success(webhookService.regenerateSecret(repositoryId, webhookId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/{webhookId}/deliveries")
    public AjaxResult listDeliveries(@PathVariable Long repositoryId, @PathVariable Long webhookId)
    {
        return success(webhookService.listDeliveries(repositoryId, webhookId));
    }

    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @PostMapping("/{webhookId}/test")
    public AjaxResult test(@PathVariable Long repositoryId, @PathVariable Long webhookId)
    {
        webhookService.sendTestPing(repositoryId, webhookId);
        return success();
    }
}
