package com.cat2bug.web.controller.system;

import java.util.List;

import com.cat2bug.common.annotation.Log;
import com.cat2bug.common.core.controller.BaseController;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.enums.BusinessType;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.domain.SysGitRepositoryMember;
import com.cat2bug.system.domain.git.GitRepoMembersFromRepoResult;
import com.cat2bug.system.domain.git.GitRepoMyAccess;
import com.cat2bug.system.domain.git.SysGitRemoteListRequest;
import com.cat2bug.system.domain.git.SysGitRemoteRepository;
import com.cat2bug.system.domain.git.SysGitRepoInviteCreateRequest;
import com.cat2bug.system.domain.git.SysGitRepoMembersFromRepoRequest;
import com.cat2bug.system.domain.git.SysGitRepositoryImportRequest;
import com.cat2bug.system.domain.git.SysGitRepositoryMemberRequest;
import com.cat2bug.system.service.ISysGitRepositoryMemberService;
import com.cat2bug.system.service.ISysGitRepositoryService;
import com.cat2bug.system.service.ISysMemberInviteService;
import com.cat2bug.web.util.InviteLinkUrlSupport;
import jakarta.servlet.http.HttpServletRequest;
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
 * REST 挂载在 {@code /system/git/repo}；由本 Controller 负责元数据 CRUD、成员 ACL 与磁盘 bare 仓生命周期，
 * 不涉及 Smart HTTP（clone/push 协议见后续任务 3）。
 *
 * clone 地址形态：{@code /{teamKey}/{projectKey}/code/{slug}.git}（见 {@link SysGitRepository#getCloneUrl()}）。
 *
 * 权限：{@code @PreAuthorize} 仅做入口层的粗粒度过滤（菜单权限字符串）；仓级有效权限由
 * {@link com.cat2bug.system.service.git.GitRepositoryAccessService} 判定（ACL 优先，否则项目角色）。
 *
 * @author cat2bug
 */
@RestController
@RequestMapping("/system/git/repo")
public class SysGitRepositoryController extends BaseController
{
    @Autowired
    private ISysGitRepositoryService sysGitRepositoryService;

    @Autowired
    private ISysGitRepositoryMemberService sysGitRepositoryMemberService;

    @Autowired
    private ISysMemberInviteService sysMemberInviteService;

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
     * 项目级 Git 概览（pull 及以上权限）：仓库数、open PR 数、关注合计、语言分布（design.md D1）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/project/{projectId}/overview")
    public AjaxResult projectOverview(@PathVariable Long projectId)
    {
        return success(sysGitRepositoryService.selectProjectOverview(projectId));
    }

    /**
     * 项目下各仓库 ACL 成员去重预览（源码成员，非项目成员；pull 及以上权限）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/project/{projectId}/members")
    public AjaxResult projectMembers(@PathVariable Long projectId)
    {
        return success(sysGitRepositoryService.selectProjectGitMembers(projectId));
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

    /**
     * 当前用户对该仓库的有效权限（供前端 Tab / settings 守卫）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull') || @ss.hasPermi('system:git:repo:manage')")
    @GetMapping("/{repositoryId}/my-access")
    public AjaxResult myAccess(@PathVariable Long repositoryId)
    {
        GitRepoMyAccess access = sysGitRepositoryMemberService.getMyAccess(repositoryId);
        return success(access);
    }

    /**
     * 列出仓库 ACL 成员（需 effective manage）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/{repositoryId}/members")
    public AjaxResult listMembers(@PathVariable Long repositoryId)
    {
        List<SysGitRepositoryMember> list = sysGitRepositoryMemberService.selectMembersByRepositoryId(repositoryId);
        return success(list);
    }

    /**
     * 添加仓库成员（可邀请非项目成员；需 effective manage）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库成员", businessType = BusinessType.INSERT)
    @PostMapping("/{repositoryId}/members")
    public AjaxResult addMember(@PathVariable Long repositoryId, @RequestBody SysGitRepositoryMemberRequest body)
    {
        Long userId = body != null ? body.getUserId() : null;
        String role = body != null ? body.getRole() : null;
        return toAjax(sysGitRepositoryMemberService.insertMember(repositoryId, userId, role));
    }

    /**
     * 修改成员角色（需 effective manage；拒绝导致零 admin 的降权）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库成员", businessType = BusinessType.UPDATE)
    @PutMapping("/{repositoryId}/members/{userId}")
    public AjaxResult updateMember(@PathVariable Long repositoryId, @PathVariable Long userId,
            @RequestBody SysGitRepositoryMemberRequest body)
    {
        String role = body != null ? body.getRole() : null;
        return toAjax(sysGitRepositoryMemberService.updateMemberRole(repositoryId, userId, role));
    }

    /**
     * 移除成员（需 effective manage；拒绝删除最后一位 admin）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库成员", businessType = BusinessType.DELETE)
    @DeleteMapping("/{repositoryId}/members/{userId}")
    public AjaxResult removeMember(@PathVariable Long repositoryId, @PathVariable Long userId)
    {
        return toAjax(sysGitRepositoryMemberService.deleteMember(repositoryId, userId));
    }

    /**
     * 列出当前有效的仓库 ACL 邀请链接（需 effective manage）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/{repositoryId}/invites")
    public AjaxResult listInvites(@PathVariable Long repositoryId, HttpServletRequest httpRequest)
    {
        return success(sysMemberInviteService.listActiveRepoInviteLinks(
                repositoryId, InviteLinkUrlSupport.resolveInviteBaseUrl(httpRequest)));
    }

    /**
     * 创建仓库 ACL 邀请链接（固定 3 天有效；需 effective manage）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库邀请链接", businessType = BusinessType.INSERT)
    @PostMapping("/{repositoryId}/invites")
    public AjaxResult createInvite(@PathVariable Long repositoryId,
            @RequestBody SysGitRepoInviteCreateRequest body,
            HttpServletRequest httpRequest)
    {
        String role = body != null ? body.getRole() : null;
        return success(sysMemberInviteService.createRepoInviteLink(
                repositoryId, role, getUserId(), InviteLinkUrlSupport.resolveInviteBaseUrl(httpRequest)));
    }

    /**
     * 吊销仓库 ACL 邀请链接（需 effective manage）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库邀请链接", businessType = BusinessType.DELETE)
    @PostMapping("/{repositoryId}/invites/{inviteId}/revoke")
    public AjaxResult revokeInvite(@PathVariable Long repositoryId, @PathVariable Long inviteId)
    {
        return toAjax(sysMemberInviteService.revokeRepoInviteLink(repositoryId, inviteId));
    }

    /**
     * 可作为「从仓邀请」源的仓库列表（同项目、非本仓、调用方可 manage）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/{repositoryId}/invite-sources")
    public AjaxResult listInviteSources(@PathVariable Long repositoryId)
    {
        return success(sysGitRepositoryMemberService.listInviteSources(repositoryId));
    }

    /**
     * 列出源仓 ACL 成员（供勾选导入）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @GetMapping("/{repositoryId}/invite-sources/{sourceRepositoryId}/members")
    public AjaxResult listInviteSourceMembers(@PathVariable Long repositoryId,
            @PathVariable Long sourceRepositoryId)
    {
        return success(sysGitRepositoryMemberService.listInviteSourceMembers(repositoryId, sourceRepositoryId));
    }

    /**
     * 从同项目其他仓库批量导入成员（跳过已是目标仓成员的用户）
     */
    @PreAuthorize("@ss.hasPermi('system:git:repo:manage') || @ss.hasPermi('system:git:repo:list') || @ss.hasPermi('system:git:repo:pull')")
    @Log(title = "仓库成员", businessType = BusinessType.INSERT)
    @PostMapping("/{repositoryId}/members/from-repo")
    public AjaxResult addMembersFromRepo(@PathVariable Long repositoryId,
            @RequestBody SysGitRepoMembersFromRepoRequest body)
    {
        Long sourceRepositoryId = body != null ? body.getSourceRepositoryId() : null;
        List<Long> userIds = body != null ? body.getUserIds() : null;
        String role = body != null ? body.getRole() : null;
        GitRepoMembersFromRepoResult result = sysGitRepositoryMemberService.insertMembersFromRepo(
                repositoryId, sourceRepositoryId, userIds, role);
        return success(result);
    }
}
