package com.cat2bug.web.controller.system;

import java.util.List;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.git.SysGitRemoteListRequest;
import com.cat2bug.system.domain.git.SysGitRemoteRepository;
import com.cat2bug.system.domain.git.SysGitRepositoryImportRequest;
import com.cat2bug.system.service.ISysGitRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目代码仓库 Controller
 *
 * REST 挂载在 {@code /system/git/repo}；由本 Controller 负责元数据 CRUD 与磁盘 bare 仓生命周期，
 * 不涉及 Smart HTTP（clone/push 协议见后续任务 3）。
 *
 * clone 地址形态：{@code /{teamKey}/{projectKey}/code/{slug}.git}（见 {@link SysGitRepository#getCloneUrl()}）。
 *
 * 权限：{@code @PreAuthorize} 仅做入口层的粗粒度过滤（菜单权限字符串）；真正的「项目角色」校验
 * 在 {@link ISysGitRepositoryService} 内按目标 projectId 显式判定，系统管理员/团队管理员若未被
 * 赋予项目角色一律拒绝，详见 design.md D3。
 *
 * @author cat2bug
 */
@RestController
@RequestMapping("/system/git/repo")
public class SysGitRepositoryController extends BaseController
{
    @Autowired
    private ISysGitRepositoryService sysGitRepositoryService;

    /**
     * 按项目查询仓库列表（pull 及以上权限）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/list")
    public AjaxResult list(SysGitRepository sysGitRepository)
    {
        List<SysGitRepository> list = sysGitRepositoryService.selectSysGitRepositoryList(sysGitRepository);
        return success(list);
    }

    /**
     * 获取仓库详细信息（pull 及以上权限）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/{repositoryId}")
    public AjaxResult getInfo(@PathVariable("repositoryId") Long repositoryId)
    {
        return success(sysGitRepositoryService.selectSysGitRepositoryByRepositoryId(repositoryId));
    }

    /**
     * 新增仓库：默认磁盘路径 {@code {cat2bug.git.root}/{teamKey}/{projectKey}/{slug}.git}（manage 权限）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "代码仓库", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysGitRepository sysGitRepository)
    {
        return success(sysGitRepositoryService.insertSysGitRepository(sysGitRepository));
    }

    /**
     * 从远端 URL 一次性导入为本地 bare 仓（manage 权限）。
     * 操作日志排除 token/username，避免 PAT 写入 oper_log。
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "代码仓库导入", businessType = BusinessType.INSERT, excludeParamNames = { "token", "username" })
    @PostMapping("/import")
    public AjaxResult importRepository(@RequestBody SysGitRepositoryImportRequest request)
    {
        return success(sysGitRepositoryService.importSysGitRepository(request));
    }

    /**
     * 使用会话内 PAT 列远端仓库（GitHub / GitLab / Gitee；manage 权限）。token 不持久化。
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @PostMapping("/remote/list")
    public AjaxResult listRemote(@RequestBody SysGitRemoteListRequest request)
    {
        List<SysGitRemoteRepository> list = sysGitRepositoryService.listRemoteRepositories(request);
        return success(list);
    }

    /**
     * 修改仓库：slug 重命名会联动迁移磁盘目录（若此前走默认路径规则），响应体在发生重命名时
     * 附带 {@code cloneUrlChanged=true} 与提示信息，告知调用方旧 clone 地址已失效（manage 权限）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "代码仓库", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysGitRepository sysGitRepository)
    {
        SysGitRepository updated = sysGitRepositoryService.updateSysGitRepository(sysGitRepository);
        AjaxResult result = success(updated);
        result.put("cloneUrlChanged", updated.isSlugChanged());
        if (updated.isSlugChanged())
        {
            result.put("message", MessageUtils.message("git.repo.slug-changed-hint"));
        }
        return result;
    }

    /**
     * 删除仓库（manage 权限）：{@code wipeDisk} 默认 true，同步清空磁盘上的 bare 仓目录；
     * 传 false 仅删除元数据，磁盘目录保留（便于误删恢复或人工归档）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage')")
    @Log(title = "代码仓库", businessType = BusinessType.DELETE)
    @DeleteMapping("/{repositoryIds}")
    public AjaxResult remove(@PathVariable Long[] repositoryIds,
        @RequestParam(value = "wipeDisk", defaultValue = "true") boolean wipeDisk)
    {
        return toAjax(sysGitRepositoryService.deleteSysGitRepositoryByRepositoryIds(repositoryIds, wipeDisk));
    }

    /**
     * 关注 / 取消关注仓库（pull 及以上权限）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "代码仓库", businessType = BusinessType.UPDATE)
    @PostMapping("/{repositoryId}/collect")
    public AjaxResult collect(@PathVariable Long repositoryId, @RequestBody SysGitRepository body)
    {
        boolean collect = body != null && Boolean.TRUE.equals(body.getCollect());
        return toAjax(sysGitRepositoryService.collectSysGitRepository(repositoryId, collect));
    }
}
