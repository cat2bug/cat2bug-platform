package com.cat2bug.admin.mcp;

import com.cat2bug.common.constant.SecurityConstants;
import com.cat2bug.common.domain.SysGitRepository;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.security.McpModules;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.service.mcp.McpProjectAccess;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.mcp.server.McpConnection;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 授权项目信息类工具：项目与模块直接按（用户，项目）计算，仓库列表经网页接口转发。
 */
public class McpProjectTools {

    private static final Pattern NAMESPACED = Pattern.compile("/([^/]+)/([^/]+)/code/([^/]+?)(?:\\.git)?/?$");
    private static final Pattern LEGACY = Pattern.compile("/git/p/(\\d+)/([^/]+?)(?:\\.git)?/?$");

    @Inject
    McpContext context;

    @Inject
    McpGateway gateway;

    @Inject
    SysGitRepositoryMapper repositoryMapper;

    @Inject
    McpJson json;

    /** 软规则提示：与 mcp-guide/zh-CN/code.md「仓库确认 / 基线分支」一致，供 AI 少打断。 */
    static final String REPOSITORY_CONFIRM_HINT =
            "单仓库且本地 origin 能 resolve 到该仓时不要追问仓库；基线默认用 defaultBranch，说明或确认基线时列出本地常见分支。"
                    + "多仓或 remote 对不上必须问用户。详见 get_guide(topic=code)。";

    @Tool(name = "get_current_project",
            description = "查看本次授权绑定的项目：项目 ID、团队标识 / 项目标识、名称、你在该项目可用的模块，以及项目下的代码仓库。"
                    + "所有工具都只作用于这个项目；要换项目需在 AI 客户端中重新登录授权。"
                    + "返回 repositoryConfirmHint：单仓且 remote 对得上时不要反复追问仓库；基线见 defaultBranch，确认时列出本地常见分支。",
            annotations = @Tool.Annotations(title = "Get current project", readOnlyHint = true, openWorldHint = false))
    String getCurrentProject(McpConnection connection) {
        McpScope scope = context.scope(connection, List.of());
        ObjectNode o = projectNode(scope.access().project());
        o.set("modules", json.valueToTree(usableModules(scope.access())));
        o.put("repositoryConfirmHint", REPOSITORY_CONFIRM_HINT);
        if (scope.access().allowsAny(List.of(McpCodeTools.LIST, McpCodeTools.PULL))) {
            o.set("repositories", McpCodeTools.repositories(scope, gateway, json));
        }
        return json.write(o);
    }

    @Tool(name = "resolve_project_by_git_remote",
            description = "根据 git remote 地址（git remote get-url origin 的输出）找到授权项目中对应的代码仓库。"
                    + "只识别本平台托管的仓库地址（…/团队标识/项目标识/code/仓库.git），且仓库必须属于授权项目。",
            annotations = @Tool.Annotations(title = "Resolve project by git remote", readOnlyHint = true, openWorldHint = false))
    String resolveProjectByGitRemote(McpConnection connection,
                                     @ToolArg(name = "remote_url", description = "git remote 地址，http(s) 形式") String remoteUrl) {
        McpScope scope = context.scope(connection, List.of());
        SysGitRepository repo = parseRemote(remoteUrl);
        if (repo == null) {
            throw new ToolCallException("不是本平台托管的仓库地址，或仓库不存在：" + remoteUrl);
        }
        if (!Objects.equals(repo.getProjectId(), scope.projectId())) {
            throw new ToolCallException("该仓库不属于本次授权的项目。如需操作其他项目，请在 AI 客户端中重新登录授权并选择对应项目");
        }
        ObjectNode o = projectNode(scope.access().project());
        o.put("repositoryId", repo.getRepositoryId());
        o.put("repositoryName", repo.getName());
        o.put("repositorySlug", repo.getSlug());
        o.set("modules", json.valueToTree(usableModules(scope.access())));
        return json.write(o);
    }

    SysGitRepository parseRemote(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return null;
        }
        String path;
        try {
            path = URI.create(remoteUrl.trim()).getPath();
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (path == null) {
            return null;
        }
        Matcher legacy = LEGACY.matcher(path);
        if (legacy.find()) {
            return repositoryMapper.selectSysGitRepositoryByProjectIdAndSlug(Long.valueOf(legacy.group(1)), legacy.group(2));
        }
        Matcher ns = NAMESPACED.matcher(path);
        if (ns.find()) {
            return repositoryMapper.selectSysGitRepositoryByTeamKeyAndProjectKeyAndSlug(ns.group(1), ns.group(2), ns.group(3));
        }
        return null;
    }

    private ObjectNode projectNode(SysProject p) {
        ObjectNode o = json.object();
        o.put("projectId", p.getProjectId());
        if (p.getTeamKey() != null && p.getProjectKey() != null) {
            o.put("ref", p.getTeamKey() + "/" + p.getProjectKey());
        }
        o.put("projectName", p.getProjectName());
        if (p.getTeamName() != null) {
            o.put("teamName", p.getTeamName());
        }
        return o;
    }

    /** 已勾选、且角色在该模块下至少有一个权限的模块。 */
    static List<String> usableModules(McpProjectAccess access) {
        boolean all = access.admin() || access.rolePermissions().contains(SecurityConstants.ALL_PERMISSION);
        return McpModules.ALL.stream()
                .filter(m -> access.moduleSelected(m.key()))
                .filter(m -> all || access.rolePermissions().stream()
                        .anyMatch(p -> key(p).equals(m.key())))
                .map(McpModules.Module::key)
                .toList();
    }

    private static String key(String permission) {
        String k = McpModules.moduleOf(permission);
        return k == null ? "" : k;
    }
}
