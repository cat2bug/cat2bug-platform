package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.SysDefect;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.mcp.server.McpConnection;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 缺陷相关工具。全部经 {@link McpGateway} 转发到网页接口；流转类工具另外镜像网页按钮的「处理人」规则。
 */
public class McpDefectTools {

    static final int MAX_PAGE_SIZE = 100;

    public record CommitArg(Long repository_id, String sha, String url, String message) {
    }

    @Inject
    McpContext context;

    @Inject
    McpGateway gateway;

    @Inject
    McpObjects objects;

    @Inject
    McpJson json;

    @Inject
    McpIdempotency idempotency;

    @Tool(name = "search_defects",
            description = "查询项目内的缺陷列表（按创建倒序）。state 取值 PROCESSING(处理中)/AUDIT(待审核)/REJECTED(已驳回)/CLOSED(已关闭)；"
                    + "type 取值 BUG/TASK/DEMAND。assigned_to_me=true 只看当前处理人包含我的缺陷。",
            annotations = @Tool.Annotations(title = "Search defects", readOnlyHint = true, openWorldHint = false))
    String searchDefects(McpConnection connection,
                         @ToolArg(name = "keyword", description = "按缺陷标题模糊匹配", required = false) String keyword,
                         @ToolArg(name = "state", description = "缺陷状态", required = false) String state,
                         @ToolArg(name = "type", description = "缺陷类型", required = false) String type,
                         @ToolArg(name = "module_id", description = "交付物（模块）ID", required = false) Long moduleId,
                         @ToolArg(name = "assigned_to_me", description = "只看指派给我的", required = false) Boolean assignedToMe,
                         @ToolArg(name = "page", description = "页码，从 1 开始", required = false) Integer page,
                         @ToolArg(name = "page_size", description = "每页条数，默认 20，最大 100", required = false) Integer pageSize) {
        McpScope scope = context.scope(connection, "system:defect:list");
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("projectId", scope.projectId());
        q.put("defectName", blankToNull(keyword));
        q.put("defectState", McpJson.stateCode(state));
        q.put("defectType", McpJson.typeCode(type));
        q.put("moduleId", moduleId);
        if (Boolean.TRUE.equals(assignedToMe)) {
            q.put("handleById", scope.userId());
        }
        q.put("pageNum", page == null || page < 1 ? 1 : page);
        q.put("pageSize", clampPageSize(pageSize));
        return json.write(json.page(gateway.get(scope, "/system/defect/list", q), json::defectSummary));
    }

    @Tool(name = "get_defect",
            description = "读取单个缺陷的详情与处理记录（含评论、关联提交）。defect_id 与 defect_num（列表里的 #编号）二选一。",
            annotations = @Tool.Annotations(title = "Get defect", readOnlyHint = true, openWorldHint = false))
    String getDefect(McpConnection connection,
                     @ToolArg(name = "defect_id", description = "缺陷 ID", required = false) Long defectId,
                     @ToolArg(name = "defect_num", description = "项目内缺陷编号", required = false) Long defectNum,
                     @ToolArg(name = "log_limit", description = "返回最近多少条处理记录，默认 20，0 表示不要", required = false) Integer logLimit) {
        McpScope scope = context.scope(connection, "system:defect:query");
        SysDefect defect = locate(scope, defectId, defectNum);
        JsonNode data = McpJson.data(gateway.get(scope, "/system/defect/" + defect.getDefectId(), null));
        ObjectNode out = json.defectDetail(data == null ? json.valueToTree(defect) : data);
        int limit = logLimit == null ? 20 : Math.max(0, Math.min(logLimit, MAX_PAGE_SIZE));
        if (limit > 0 && scope.access().allowsAny(List.of("system:defect:list", "system:defect:log:list"))) {
            ArrayNode logs = out.putArray("logs");
            JsonNode table = gateway.get(scope, "/system/log/list",
                    Map.of("defectId", defect.getDefectId(), "pageNum", 1, "pageSize", limit));
            if (table != null && table.get("rows") instanceof ArrayNode rows) {
                rows.forEach(r -> logs.add(json.defectLog(r)));
            }
        }
        return json.write(out);
    }

    @Tool(name = "create_defect",
            description = "新建缺陷。handle_by 为处理人用户 ID 列表（可先用 list_project_members 查询）。"
                    + "建议传 idempotency_key，重试时不会重复创建。",
            annotations = @Tool.Annotations(title = "Create defect", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String createDefect(McpConnection connection,
                        @ToolArg(name = "name", description = "缺陷标题") String name,
                        @ToolArg(name = "describe", description = "缺陷描述，支持 Markdown", required = false) String describe,
                        @ToolArg(name = "type", description = "BUG / TASK / DEMAND，默认 BUG", required = false) String type,
                        @ToolArg(name = "level", description = "优先级 urgent / height / middle / low，默认 middle", required = false) String level,
                        @ToolArg(name = "module_id", description = "交付物（模块）ID", required = false) Long moduleId,
                        @ToolArg(name = "module_version", description = "版本号", required = false) String moduleVersion,
                        @ToolArg(name = "handle_by", description = "处理人用户 ID 列表", required = false) List<Long> handleBy,
                        @ToolArg(name = "idempotency_key", description = "幂等键（字母数字 ._:-，≤64）", required = false) String idempotencyKey) {
        McpScope scope = context.scope(connection, "system:defect:add");
        if (name == null || name.isBlank()) {
            throw new ToolCallException("缺陷标题不能为空");
        }
        if (moduleId != null) {
            objects.module(scope, moduleId);
        }
        McpIdempotency.Outcome outcome = idempotency.run(scope.userId(), "create_defect", idempotencyKey, () -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("projectId", scope.projectId());
            body.put("defectName", name.trim());
            body.put("defectDescribe", describe);
            body.put("defectType", McpJson.typeCode(type));
            body.put("defectLevel", normalizeLevel(level));
            body.put("moduleId", moduleId);
            body.put("moduleVersion", blankToNull(moduleVersion));
            body.put("handleBy", handleBy == null ? List.of() : handleBy);
            JsonNode data = McpJson.data(gateway.post(scope, "/system/defect", body));
            if (data == null || !data.hasNonNull("defectId")) {
                throw new ToolCallException("新建缺陷失败：接口未返回缺陷 ID");
            }
            return data.get("defectId").asText();
        });
        return json.write(defectResult(scope, Long.valueOf(outcome.resultId()), outcome.replayed()));
    }

    @Tool(name = "update_defect",
            description = "修改缺陷的标题、描述、类型、优先级、交付物或版本。只传需要改的字段；状态与处理人请用流转工具修改。",
            annotations = @Tool.Annotations(title = "Update defect", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String updateDefect(McpConnection connection,
                        @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                        @ToolArg(name = "name", description = "缺陷标题", required = false) String name,
                        @ToolArg(name = "describe", description = "缺陷描述", required = false) String describe,
                        @ToolArg(name = "type", description = "BUG / TASK / DEMAND", required = false) String type,
                        @ToolArg(name = "level", description = "urgent / height / middle / low", required = false) String level,
                        @ToolArg(name = "module_id", description = "交付物（模块）ID", required = false) Long moduleId,
                        @ToolArg(name = "module_version", description = "版本号", required = false) String moduleVersion) {
        McpScope scope = context.scope(connection, "system:defect:edit");
        SysDefect defect = objects.defect(scope, defectId);
        if (moduleId != null) {
            objects.module(scope, moduleId);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("defectId", defect.getDefectId());
        body.put("projectId", scope.projectId());
        body.put("defectName", name == null || name.isBlank() ? defect.getDefectName() : name.trim());
        body.put("defectDescribe", describe != null ? describe : defect.getDefectDescribe());
        body.put("defectType", type != null ? McpJson.typeCode(type) : defect.getDefectType());
        body.put("defectLevel", level != null ? normalizeLevel(level) : defect.getDefectLevel());
        body.put("moduleId", moduleId != null ? moduleId : defect.getModuleId());
        body.put("moduleVersion", moduleVersion != null ? moduleVersion : defect.getModuleVersion());
        body.put("defectState", defect.getDefectState());
        body.put("customFields", defect.getCustomFields());
        gateway.put(scope, "/system/defect", body);
        return json.write(defectResult(scope, defect.getDefectId(), false));
    }

    @Tool(name = "comment_defect",
            description = "在缺陷的处理记录下发表评论。默认评论最新一条处理记录，也可指定 defect_log_id。",
            annotations = @Tool.Annotations(title = "Comment defect", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String commentDefect(McpConnection connection,
                         @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                         @ToolArg(name = "content", description = "评论内容") String content,
                         @ToolArg(name = "defect_log_id", description = "评论哪条处理记录", required = false) Long defectLogId,
                         @ToolArg(name = "idempotency_key", description = "幂等键", required = false) String idempotencyKey) {
        McpScope scope = context.scope(connection, "system:comment:add", "system:defect:list");
        SysDefect defect = objects.defect(scope, defectId);
        if (content == null || content.isBlank()) {
            throw new ToolCallException("评论内容不能为空");
        }
        Long logId = defectLogId != null ? objects.defectLog(scope, defectLogId).getDefectLogId()
                : latestLogId(scope, defect.getDefectId());
        if (logId == null) {
            throw new ToolCallException("该缺陷还没有处理记录，无法评论");
        }
        McpIdempotency.Outcome outcome = idempotency.run(scope.userId(), "comment_defect", idempotencyKey, () -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("commentContent", content);
            body.put("moduleType", "defect_log");
            body.put("correlationId", logId);
            JsonNode res = gateway.post(scope, "/system/comment", body);
            JsonNode data = McpJson.data(res);
            return data != null && data.hasNonNull("commentId") ? data.get("commentId").asText() : String.valueOf(logId);
        });
        ObjectNode out = json.object();
        out.put("defectId", defect.getDefectId());
        out.put("defectLogId", logId);
        out.put("replayed", outcome.replayed());
        return json.write(out);
    }

    @Tool(name = "assign_defect",
            description = "把缺陷指派给其他成员。要求你是该缺陷的创建人或当前处理人，且缺陷未关闭。流转规则见 get_guide(topic=defect)。",
            annotations = @Tool.Annotations(title = "Assign defect", readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    String assignDefect(McpConnection connection,
                        @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                        @ToolArg(name = "handle_by", description = "新的处理人用户 ID 列表") List<Long> handleBy,
                        @ToolArg(name = "remark", description = "说明", required = false) String remark) {
        McpScope scope = context.scope(connection, "system:defect:assign");
        SysDefect defect = objects.defect(scope, defectId);
        if (!Objects.equals(defect.getCreateById(), scope.userId()) && !isHandler(defect, scope)) {
            throw new ToolCallException("只有缺陷的创建人或当前处理人才能指派");
        }
        if (handleBy == null || handleBy.isEmpty()) {
            throw new ToolCallException("handle_by 不能为空");
        }
        return transition(scope, defect, "assign", handleBy, remark, null);
    }

    @Tool(name = "repair_defect",
            description = "提交修复，缺陷进入待审核。要求你是当前处理人且状态为处理中或已驳回。"
                    + "review_by 为审核人（默认取缺陷创建人，即提出 bug 的人）；commits 可关联修复提交："
                    + "本平台托管仓库传 repository_id + sha（会校验提交存在），外部仓库传 sha + url。流转规则见 get_guide(topic=defect)。",
            annotations = @Tool.Annotations(title = "Repair defect", readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    String repairDefect(McpConnection connection,
                        @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                        @ToolArg(name = "remark", description = "修复说明", required = false) String remark,
                        @ToolArg(name = "review_by", description = "审核人用户 ID（默认缺陷创建人）", required = false) Long reviewBy,
                        @ToolArg(name = "commits", description = "关联提交列表", required = false) List<CommitArg> commits) {
        McpScope scope = context.scope(connection, "system:defect:repair");
        SysDefect defect = objects.defect(scope, defectId);
        requireHandler(scope, defect);
        Long reviewer = reviewBy != null ? reviewBy : defect.getCreateById();
        if (reviewer == null) {
            reviewer = latestLogCreator(scope, defect.getDefectId());
        }
        List<Map<String, Object>> commitBody = new ArrayList<>();
        if (commits != null) {
            for (CommitArg c : commits) {
                if (c == null) {
                    continue;
                }
                if (c.repository_id() != null) {
                    objects.repository(scope, c.repository_id());
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("repositoryId", c.repository_id());
                m.put("commitSha", c.sha());
                m.put("commitUrl", c.url());
                m.put("commitMessage", c.message());
                commitBody.add(m);
            }
        }
        return transition(scope, defect, "repair", reviewer == null ? null : List.of(reviewer), remark, commitBody);
    }

    @Tool(name = "reject_defect",
            description = "驳回修复，缺陷退回给上一次的处理人。要求你是当前处理人且状态为待审核。流转规则见 get_guide(topic=defect)。",
            annotations = @Tool.Annotations(title = "Reject defect", readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    String rejectDefect(McpConnection connection,
                        @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                        @ToolArg(name = "remark", description = "驳回原因") String remark) {
        McpScope scope = context.scope(connection, "system:defect:reject");
        SysDefect defect = objects.defect(scope, defectId);
        requireHandler(scope, defect);
        return transition(scope, defect, "reject", null, remark, null);
    }

    @Tool(name = "pass_defect",
            description = "审核通过，缺陷关闭。要求你是当前处理人且状态为待审核。流转规则见 get_guide(topic=defect)。",
            annotations = @Tool.Annotations(title = "Pass defect", readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    String passDefect(McpConnection connection,
                      @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                      @ToolArg(name = "remark", description = "说明", required = false) String remark) {
        McpScope scope = context.scope(connection, "system:defect:pass");
        SysDefect defect = objects.defect(scope, defectId);
        requireHandler(scope, defect);
        return transition(scope, defect, "pass", null, remark, null);
    }

    @Tool(name = "close_defect",
            description = "直接关闭缺陷（不经审核）。状态须为处理中、待审核或已驳回。",
            annotations = @Tool.Annotations(title = "Close defect", readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false))
    String closeDefect(McpConnection connection,
                       @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                       @ToolArg(name = "remark", description = "关闭原因") String remark) {
        McpScope scope = context.scope(connection, "system:defect:close");
        SysDefect defect = objects.defect(scope, defectId);
        return transition(scope, defect, "close", null, remark, null);
    }

    @Tool(name = "open_defect",
            description = "重新开启已关闭的缺陷，并指定处理人。",
            annotations = @Tool.Annotations(title = "Open defect", readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    String openDefect(McpConnection connection,
                      @ToolArg(name = "defect_id", description = "缺陷 ID") Long defectId,
                      @ToolArg(name = "handle_by", description = "处理人用户 ID 列表") List<Long> handleBy,
                      @ToolArg(name = "remark", description = "重新开启的原因") String remark) {
        McpScope scope = context.scope(connection, "system:defect:open");
        SysDefect defect = objects.defect(scope, defectId);
        if (handleBy == null || handleBy.isEmpty()) {
            throw new ToolCallException("handle_by 不能为空");
        }
        return transition(scope, defect, "open", handleBy, remark, null);
    }

    @Tool(name = "list_project_members",
            description = "列出项目成员（用户 ID、账号、昵称），用于选择处理人或审核人。",
            annotations = @Tool.Annotations(title = "List project members", readOnlyHint = true, openWorldHint = false))
    String listProjectMembers(McpConnection connection,
                              @ToolArg(name = "keyword", description = "按账号或昵称过滤", required = false) String keyword) {
        McpScope scope = context.scope(connection, "system:project:list", "system:project:member");
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("pageNum", 1);
        q.put("pageSize", 500);
        q.put("params[search]", blankToNull(keyword));
        JsonNode table = gateway.get(scope, "/system/project/" + scope.projectId() + "/member", q);
        return json.write(json.page(table, json::member));
    }

    private String transition(McpScope scope, SysDefect defect, String action, List<Long> receiveBy, String remark,
                              List<Map<String, Object>> commits) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("defectId", defect.getDefectId());
        body.put("defectLogDescribe", remark == null ? "" : remark);
        if (receiveBy != null) {
            body.put("receiveBy", receiveBy);
        }
        if (commits != null && !commits.isEmpty()) {
            body.put("commits", commits);
        }
        JsonNode data = McpJson.data(gateway.post(scope, "/system/defect/" + defect.getDefectId() + "/" + action, body));
        ObjectNode out = defectResult(scope, defect.getDefectId(), false);
        if (data != null && !data.isNull()) {
            out.set("log", json.defectLog(data));
        }
        return json.write(out);
    }

    private ObjectNode defectResult(McpScope scope, Long defectId, boolean replayed) {
        SysDefect fresh = objects.defect(scope, defectId);
        ObjectNode out = json.defectSummary(json.valueToTree(fresh));
        out.put("replayed", replayed);
        return out;
    }

    private SysDefect locate(McpScope scope, Long defectId, Long defectNum) {
        if (defectId != null) {
            return objects.defect(scope, defectId);
        }
        if (defectNum != null) {
            return objects.defectByNum(scope, defectNum);
        }
        throw new ToolCallException("defect_id 与 defect_num 至少传一个");
    }

    private Long latestLogId(McpScope scope, Long defectId) {
        JsonNode first = firstLog(scope, defectId);
        return first == null || !first.hasNonNull("defectLogId") ? null : first.get("defectLogId").asLong();
    }

    private Long latestLogCreator(McpScope scope, Long defectId) {
        JsonNode first = firstLog(scope, defectId);
        if (first == null || !first.hasNonNull("createBy")) {
            return null;
        }
        try {
            return Long.valueOf(first.get("createBy").asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private JsonNode firstLog(McpScope scope, Long defectId) {
        JsonNode table = gateway.get(scope, "/system/log/list", Map.of("defectId", defectId, "pageNum", 1, "pageSize", 1));
        return table != null && table.get("rows") instanceof ArrayNode rows && !rows.isEmpty() ? rows.get(0) : null;
    }

    private static boolean isHandler(SysDefect defect, McpScope scope) {
        return defect.getHandleBy() != null && defect.getHandleBy().contains(scope.userId());
    }

    private static void requireHandler(McpScope scope, SysDefect defect) {
        if (!isHandler(defect, scope)) {
            throw new ToolCallException("只有缺陷的当前处理人才能执行该操作");
        }
    }

    static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }
        String l = level.trim().toLowerCase(java.util.Locale.ROOT);
        if (l.equals("high")) {
            l = "height";
        }
        if (!List.of("urgent", "height", "middle", "low").contains(l)) {
            throw new ToolCallException("优先级取值应为 urgent / height / middle / low：" + level);
        }
        return l;
    }

    static int clampPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
