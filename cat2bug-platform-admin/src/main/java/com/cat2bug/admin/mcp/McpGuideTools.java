package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysProjectDefectFieldManageItem;
import com.cat2bug.common.domain.SysRole;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.open.ApiMember;
import com.cat2bug.system.mapper.OpenApiMemberMapper;
import com.cat2bug.system.service.SysProjectDefectFieldService;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkiverse.mcp.server.McpConnection;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 使用指南：「本项目实况」（按当前用户与授权项目实时生成）+ 主题正文（{@link McpGuideContent}）。
 */
public class McpGuideTools {

    static final int MAX_MEMBERS_PER_ROLE = 30;

    record Topic(String key, String label, String module) {
    }

    static final Map<String, Topic> TOPICS = new LinkedHashMap<>();

    static {
        for (Topic t : List.of(
                new Topic("overview", "总览", null),
                new Topic("roles", "角色与成员", null),
                new Topic("defect", "缺陷", "defect"),
                new Topic("case", "测试用例", "case"),
                new Topic("plan", "测试计划", "plan"),
                new Topic("deliverable", "交付物", "deliverable"),
                new Topic("document", "文档", "document"),
                new Topic("code", "代码", "code"))) {
            TOPICS.put(t.key(), t);
        }
    }

    static final Map<String, String> MODULE_LABELS = Map.of(
            "defect", "缺陷", "case", "测试用例", "plan", "测试计划", "deliverable", "交付物",
            "document", "文档", "code", "代码", "dashboard", "仪表盘", "project", "项目");

    /** 各模块里值得告诉 AI 的操作（权限串 → 说明），按流程顺序。 */
    static final Map<String, Map<String, String>> OPERATIONS = new LinkedHashMap<>();

    static {
        OPERATIONS.put("defect", ordered(
                "system:defect:list", "查询与评论",
                "system:defect:add", "新建",
                "system:defect:edit", "修改",
                "system:defect:assign", "指派",
                "system:defect:repair", "提交修复",
                "system:defect:pass", "审核通过",
                "system:defect:reject", "驳回",
                "system:defect:close", "关闭",
                "system:defect:open", "重新开启"));
        OPERATIONS.put("case", ordered(
                "system:case:list", "查询",
                "system:case:add", "新建",
                "system:case:edit", "修改"));
        OPERATIONS.put("plan", ordered(
                "system:plan:list", "查询",
                "system:plan:run", "执行（记录结果）",
                "system:plan:add", "新建",
                "system:plan:edit", "编辑（含记录结果）"));
        OPERATIONS.put("deliverable", ordered(
                "system:module:list", "查询",
                "system:module:add", "新建",
                "system:module:edit", "修改"));
        OPERATIONS.put("document", ordered(
                "system:document:list", "查询",
                "system:document:add", "新建",
                "system:document:edit", "修改"));
        OPERATIONS.put("code", ordered(
                McpCodeTools.LIST, "查看仓库",
                McpCodeTools.PULL, "读取代码与提交"));
        OPERATIONS.put("dashboard", ordered("system:dashboard:query", "查看统计"));
        OPERATIONS.put("project", ordered("system:project:member", "查看成员"));
    }

    @Inject
    McpContext context;

    @Inject
    McpGateway gateway;

    @Inject
    McpGuideContent content;

    @Inject
    OpenApiMemberMapper memberMapper;

    @Inject
    SysProjectDefectFieldService defectFieldService;

    @Tool(name = "get_guide",
            description = "Cat2Bug 使用指南：项目实况（你的角色、可用模块与可执行操作）+ 业务流程说明（缺陷流转、每一步谁来做、用哪个工具、"
                    + "做完后该提示用户什么）。开始操作或回答业务问题前先调用；topic 取 overview（默认）/ roles / defect / case / "
                    + "plan / deliverable / document / code。",
            annotations = @Tool.Annotations(title = "Get usage guide", readOnlyHint = true, openWorldHint = false))
    String getGuide(McpConnection connection,
                    @ToolArg(name = "topic", description = "指南主题，默认 overview", required = false) String topic) {
        String key = topic == null || topic.isBlank() ? "overview" : topic.trim().toLowerCase(Locale.ROOT);
        Topic t = TOPICS.get(key);
        if (t == null) {
            throw new ToolCallException("topic 取值应为 " + String.join(" / ", TOPICS.keySet()) + "：" + topic);
        }
        McpScope scope = context.scope(connection, List.of());
        List<String> usable = McpProjectTools.usableModules(scope.access());

        StringBuilder out = new StringBuilder();
        out.append("# Cat2Bug 使用指南：").append(t.label()).append("\n\n## 本项目实况\n\n");
        facts(out, scope, usable);
        if (t.module() == null) {
            operations(out, scope, usable);
            members(out, scope, "roles".equals(key));
        } else if (!usable.contains(t.module())) {
            out.append("\n你在本项目没有『").append(t.label()).append("』模块的权限（角色未授予，或项目未把该模块开放给 MCP）。")
                    .append("如需使用，请项目管理员在网页「项目设置 → AI 管理 → MCP 接入」中开启该模块，并确认你的角色有相应权限。\n");
            return out.toString();
        } else {
            operations(out, scope, List.of(t.module()));
            if ("defect".equals(key)) {
                defectFacts(out, scope);
            } else if ("plan".equals(key)) {
                planFacts(out, scope);
            }
        }
        out.append("\n").append(content.topic(key));
        return out.toString();
    }

    private void facts(StringBuilder out, McpScope scope, List<String> usable) {
        SysProject p = scope.access().project();
        out.append("- 项目：").append(p.getProjectName());
        if (p.getTeamKey() != null && p.getProjectKey() != null) {
            out.append("（").append(p.getTeamKey()).append('/').append(p.getProjectKey()).append("）");
        }
        out.append("，项目 ID ").append(p.getProjectId()).append('\n');
        SysUser user = scope.caller().getUser();
        String nick = user != null && user.getNickName() != null ? user.getNickName() : scope.caller().getUsername();
        out.append("- 你：").append(nick).append("（账号 ").append(scope.caller().getUsername())
                .append("，用户 ID ").append(scope.userId()).append("）\n");
        List<String> roleNames = new ArrayList<>();
        if (scope.access().admin()) {
            roleNames.add("系统管理员");
        }
        for (SysRole r : scope.access().roles()) {
            if (r != null && r.getRoleName() != null && !roleNames.contains(r.getRoleName())) {
                roleNames.add(r.getRoleName());
            }
        }
        out.append("- 你的角色：").append(roleNames.isEmpty() ? "（无）" : String.join("、", roleNames)).append('\n');
        out.append("- 可用模块：").append(usable.isEmpty() ? "（无）" : String.join("、", usable.stream()
                .map(m -> MODULE_LABELS.getOrDefault(m, m)).toList())).append('\n');
    }

    private void operations(StringBuilder out, McpScope scope, List<String> modules) {
        out.append("- 你可执行的操作：\n");
        for (String m : modules) {
            Map<String, String> ops = OPERATIONS.get(m);
            if (ops == null) {
                continue;
            }
            List<String> allowed = ops.entrySet().stream()
                    .filter(e -> scope.access().allows(e.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
            out.append("  - ").append(MODULE_LABELS.getOrDefault(m, m)).append("：")
                    .append(allowed.isEmpty() ? "（无）" : String.join("、", allowed)).append('\n');
        }
    }

    private void members(StringBuilder out, McpScope scope, boolean withNames) {
        if (!scope.access().allowsAny(List.of("system:project:list", "system:project:member"))) {
            out.append("- 成员：你无权查看本项目成员名单\n");
            return;
        }
        Map<String, List<String>> byRole = new LinkedHashMap<>();
        for (ApiMember m : memberMapper.selectMemberList(scope.projectId(), new ApiMember())) {
            String name = (m.getMemberName() == null ? m.getMemberAccount() : m.getMemberName())
                    + "（ID " + m.getMemberId() + "）";
            List<String> roles = m.getRoleNameList() == null || m.getRoleNameList().isEmpty()
                    ? List.of("未分配角色") : m.getRoleNameList();
            for (String role : roles) {
                List<String> names = byRole.computeIfAbsent(role == null ? "未分配角色" : role, k -> new ArrayList<>());
                if (!names.contains(name)) {
                    names.add(name);
                }
            }
        }
        out.append("- 成员（按角色）：\n");
        byRole.forEach((role, names) -> {
            out.append("  - ").append(role).append("：").append(names.size()).append(" 人");
            if (withNames) {
                List<String> shown = names.subList(0, Math.min(names.size(), MAX_MEMBERS_PER_ROLE));
                out.append("，").append(String.join("、", shown));
                if (names.size() > shown.size()) {
                    out.append(" 等");
                }
            }
            out.append('\n');
        });
    }

    private void defectFacts(StringBuilder out, McpScope scope) {
        List<String> builtin = new ArrayList<>();
        List<String> custom = new ArrayList<>();
        for (SysProjectDefectFieldManageItem f : defectFieldService.selectManageListByProjectId(scope.projectId())) {
            if (f.getEnabled() == null || f.getEnabled() != 1) {
                continue;
            }
            if (Boolean.TRUE.equals(f.getSystemBuiltin())) {
                builtin.add(f.getFieldLabel());
            } else {
                custom.add(customField(f));
            }
        }
        out.append("- 缺陷启用的内置字段：").append(builtin.isEmpty() ? "（无）" : String.join("、", builtin)).append('\n');
        out.append("- 缺陷自定义字段：").append(custom.isEmpty() ? "（无）" : String.join("；", custom)).append('\n');
        if (scope.access().allows("system:defect:list")) {
            out.append("- 处理人包含你的缺陷：");
            List<String> counts = new ArrayList<>();
            for (String[] s : new String[][]{{"PROCESSING", "处理中"}, {"AUDIT", "待审核"}, {"REJECTED", "已驳回"}}) {
                Map<String, Object> q = new LinkedHashMap<>();
                q.put("projectId", scope.projectId());
                q.put("defectState", McpJson.stateCode(s[0]));
                q.put("handleById", scope.userId());
                q.put("pageNum", 1);
                q.put("pageSize", 1);
                JsonNode table = gateway.get(scope, "/system/defect/list", q);
                counts.add(s[1] + " " + (table == null ? 0 : table.path("total").asLong()));
            }
            out.append(String.join("，", counts)).append('\n');
        }
    }

    private static String customField(SysProjectDefectFieldManageItem f) {
        StringBuilder s = new StringBuilder(f.getFieldLabel() == null ? f.getFieldKey() : f.getFieldLabel());
        s.append("（键 ").append(f.getFieldKey()).append("，").append(f.getFieldType());
        if (f.getRequired() != null && f.getRequired() == 1) {
            s.append("，必填");
        }
        Object options = f.getTypeConfig() == null ? null : f.getTypeConfig().get("options");
        if (options instanceof List<?> list && !list.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    labels.add(m.get("key") + "=" + m.get("label"));
                }
            }
            s.append("，可选 ").append(String.join(" / ", labels));
        }
        return s.append("）").toString();
    }

    private void planFacts(StringBuilder out, McpScope scope) {
        if (!scope.access().allows("system:plan:list")) {
            return;
        }
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("projectId", scope.projectId());
        q.put("pageNum", 1);
        q.put("pageSize", McpDefectTools.MAX_PAGE_SIZE);
        JsonNode table = gateway.get(scope, "/system/plan/list", q);
        List<String> open = new ArrayList<>();
        if (table != null) {
            for (JsonNode p : table.path("rows")) {
                long left = p.path("unexecutedCount").asLong();
                if (left > 0) {
                    open.add("「" + p.path("planName").asText() + "」剩 " + left + " 条");
                }
            }
        }
        out.append("- 还有未执行子项的测试计划：").append(open.isEmpty() ? "无" : open.size() + " 个，"
                + String.join("，", open.subList(0, Math.min(open.size(), 10)))).append('\n');
    }

    private static Map<String, String> ordered(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }
}
