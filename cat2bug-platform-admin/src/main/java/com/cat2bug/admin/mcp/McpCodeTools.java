package com.cat2bug.admin.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.mcp.server.McpConnection;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.cat2bug.admin.mcp.McpDefectTools.blankToNull;

/**
 * 代码仓库只读工具：列仓库、看目录、读文件、检索、看提交。
 */
public class McpCodeTools {

    static final String LIST = "system:git:repo:list";
    static final String PULL = "system:git:repo:pull";

    @Inject
    McpContext context;

    @Inject
    McpGateway gateway;

    @Inject
    McpObjects objects;

    @Inject
    McpJson json;

    @Tool(name = "list_repositories",
            description = "列出项目下的代码仓库（ID、名称、默认分支）。",
            annotations = @Tool.Annotations(title = "List repositories", readOnlyHint = true, openWorldHint = false))
    String listRepositories(McpConnection connection) {
        McpScope scope = context.scope(connection, LIST, PULL);
        return json.write(repositories(scope, gateway, json));
    }

    static ArrayNode repositories(McpScope scope, McpGateway gateway, McpJson json) {
        JsonNode data = McpJson.data(gateway.get(scope, "/system/git/repo/list", Map.of("projectId", scope.projectId())));
        ArrayNode out = json.array();
        JsonNode rows = data != null && data.has("rows") ? data.get("rows") : data;
        if (rows instanceof ArrayNode arr) {
            arr.forEach(r -> {
                ObjectNode o = json.object();
                McpJson.copy(r, o, "repositoryId", "name", "slug", "defaultBranch", "visibility", "description");
                out.add(o);
            });
        }
        return out;
    }

    @Tool(name = "list_code_tree",
            description = "列出仓库某目录下的文件与子目录。ref 为分支、标签或提交，默认仓库默认分支。",
            annotations = @Tool.Annotations(title = "List code tree", readOnlyHint = true, openWorldHint = false))
    String listCodeTree(McpConnection connection,
                        @ToolArg(name = "repository_id", description = "仓库 ID") Long repositoryId,
                        @ToolArg(name = "path", description = "目录路径，空表示根目录", required = false) String path,
                        @ToolArg(name = "ref", description = "分支 / 标签 / 提交", required = false) String ref) {
        McpScope scope = context.scope(connection, PULL);
        objects.repository(scope, repositoryId);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("ref", blankToNull(ref));
        q.put("path", blankToNull(path));
        JsonNode data = McpJson.data(gateway.get(scope, "/system/git/repo/" + repositoryId + "/tree", q));
        ArrayNode out = json.array();
        if (data instanceof ArrayNode arr) {
            arr.forEach(e -> {
                ObjectNode o = json.object();
                McpJson.copy(e, o, "name", "path", "type", "size");
                out.add(o);
            });
        }
        return json.write(out);
    }

    @Tool(name = "read_code_file",
            description = "读取仓库中的文本文件内容（超大文件会截断，二进制文件不返回内容）。",
            annotations = @Tool.Annotations(title = "Read code file", readOnlyHint = true, openWorldHint = false))
    String readCodeFile(McpConnection connection,
                        @ToolArg(name = "repository_id", description = "仓库 ID") Long repositoryId,
                        @ToolArg(name = "path", description = "文件路径") String path,
                        @ToolArg(name = "ref", description = "分支 / 标签 / 提交", required = false) String ref) {
        McpScope scope = context.scope(connection, PULL);
        objects.repository(scope, repositoryId);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("ref", blankToNull(ref));
        q.put("path", path);
        JsonNode data = McpJson.data(gateway.get(scope, "/system/git/repo/" + repositoryId + "/blob", q));
        ObjectNode o = json.object();
        McpJson.copy(data, o, "path", "size", "truncated", "binary", "content");
        return json.write(o);
    }

    @Tool(name = "search_code",
            description = "在仓库中按行检索文本（不区分大小写的字面量匹配），返回文件路径、行号与该行内容。",
            annotations = @Tool.Annotations(title = "Search code", readOnlyHint = true, openWorldHint = false))
    String searchCode(McpConnection connection,
                      @ToolArg(name = "repository_id", description = "仓库 ID") Long repositoryId,
                      @ToolArg(name = "query", description = "要查找的文本") String query,
                      @ToolArg(name = "path", description = "只在该目录下查找", required = false) String path,
                      @ToolArg(name = "ref", description = "分支 / 标签 / 提交", required = false) String ref,
                      @ToolArg(name = "limit", description = "最多返回多少处，默认 50，最大 200", required = false) Integer limit) {
        McpScope scope = context.scope(connection, PULL);
        objects.repository(scope, repositoryId);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("q", query);
        q.put("ref", blankToNull(ref));
        q.put("path", blankToNull(path));
        q.put("limit", limit);
        return json.write(McpJson.data(gateway.get(scope, "/system/git/repo/" + repositoryId + "/search", q)));
    }

    @Tool(name = "list_commits",
            description = "列出仓库最近的提交。",
            annotations = @Tool.Annotations(title = "List commits", readOnlyHint = true, openWorldHint = false))
    String listCommits(McpConnection connection,
                       @ToolArg(name = "repository_id", description = "仓库 ID") Long repositoryId,
                       @ToolArg(name = "ref", description = "分支 / 标签 / 提交", required = false) String ref,
                       @ToolArg(name = "author", description = "按作者过滤", required = false) String author,
                       @ToolArg(name = "limit", description = "条数，默认 20", required = false) Integer limit) {
        McpScope scope = context.scope(connection, PULL);
        objects.repository(scope, repositoryId);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("ref", blankToNull(ref));
        q.put("author", blankToNull(author));
        q.put("limit", limit == null ? 20 : limit);
        return json.write(McpJson.data(gateway.get(scope, "/system/git/repo/" + repositoryId + "/commits", q)));
    }

    @Tool(name = "get_commit",
            description = "读取单个提交的说明与改动文件（含行数统计）。",
            annotations = @Tool.Annotations(title = "Get commit", readOnlyHint = true, openWorldHint = false))
    String getCommit(McpConnection connection,
                     @ToolArg(name = "repository_id", description = "仓库 ID") Long repositoryId,
                     @ToolArg(name = "sha", description = "提交 SHA（可缩写）") String sha) {
        McpScope scope = context.scope(connection, PULL);
        objects.repository(scope, repositoryId);
        JsonNode data = McpJson.data(gateway.get(scope,
                "/system/git/repo/" + repositoryId + "/commits/" + McpGateway.encode(sha == null ? "" : sha.trim()), null));
        ObjectNode o = json.object();
        McpJson.copy(data, o, "id", "shortId", "fullMessage", "authorName", "authorEmail", "commitTime", "stats");
        if (data != null && data.get("files") instanceof ArrayNode files) {
            ArrayNode out = o.putArray("files");
            files.forEach(f -> {
                ObjectNode fo = json.object();
                McpJson.copy(f, fo, "changeType", "oldPath", "newPath", "path", "additions", "deletions", "binary");
                out.add(fo);
            });
        }
        return json.write(o);
    }
}
