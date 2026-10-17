package com.cat2bug.admin.mcp;

import com.cat2bug.admin.mcp.McpOperationCatalog.Operation;
import com.cat2bug.admin.mcp.McpOperationCatalog.Param;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.mcp.server.McpConnection;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.inject.Inject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 通用通道：精做工具没覆盖的网页操作，按目录列出、查看参数后调用。仍走同一套权限与归属校验。
 */
public class McpGenericTools {

    static final int MAX_RESULT_CHARS = 60_000;
    private static final Pattern SAFE_PATH_VALUE = Pattern.compile("^[A-Za-z0-9_.,\\-]{1,200}$");
    private static final Pattern VAR = Pattern.compile("\\{([A-Za-z0-9_]+)}");

    /** 出现在查询参数或请求体顶层时，不需要做归属校验的 *Id 字段 */
    static final Set<String> BENIGN_ID_KEYS = Set.of("createById", "updateById", "caseStepId", "parentId",
            "userId", "memberId", "roleId", "roleIds", "correlationId");

    @Inject
    McpContext context;

    @Inject
    McpGateway gateway;

    @Inject
    McpObjects objects;

    @Inject
    McpJson json;

    @Tool(name = "list_operations",
            description = "通用通道：列出你在授权项目中可调用的网页操作（精做工具未覆盖时使用）。可按模块过滤："
                    + "defect / case / plan / deliverable / document / code / dashboard / project。",
            annotations = @Tool.Annotations(title = "List operations", readOnlyHint = true, openWorldHint = false))
    String listOperations(McpConnection connection,
                          @ToolArg(name = "module", description = "模块", required = false) String module) {
        McpScope scope = context.scope(connection, List.of());
        ArrayNode out = json.array();
        for (Operation op : McpOperationCatalog.operations().values()) {
            if (module != null && !module.isBlank() && !op.modules().contains(module.trim())) {
                continue;
            }
            if (!allowed(scope, op)) {
                continue;
            }
            ObjectNode o = json.object();
            o.put("operation", op.id());
            o.put("method", op.httpMethod());
            o.put("path", op.path());
            o.put("readOnly", op.readOnly());
            out.add(o);
        }
        return json.write(out);
    }

    @Tool(name = "describe_operation",
            description = "通用通道：查看某个操作的路径参数、查询参数、请求体字段与所需权限。",
            annotations = @Tool.Annotations(title = "Describe operation", readOnlyHint = true, openWorldHint = false))
    String describeOperation(McpConnection connection,
                             @ToolArg(name = "operation", description = "list_operations 返回的 operation") String operation) {
        McpScope scope = context.scope(connection, List.of());
        Operation op = require(scope, operation);
        ObjectNode o = json.object();
        o.put("operation", op.id());
        o.put("method", op.httpMethod());
        o.put("path", op.path());
        o.put("readOnly", op.readOnly());
        o.set("permissions", json.valueToTree(op.permissions()));
        o.put("permissionLogic", op.anyOf() ? "any" : "all");
        o.set("params", json.valueToTree(op.params()));
        if (op.bodyType() != null) {
            o.put("bodyType", op.bodyType());
            o.set("bodyFields", json.valueToTree(op.bodyFields()));
        }
        o.put("note", "projectId 会自动填为授权项目；路径或请求体里的对象 ID 必须属于该项目");
        return json.write(o);
    }

    @Tool(name = "call_operation",
            description = "通用通道：调用某个网页操作。path_params 填路径变量，query 填查询参数，body 为 JSON 请求体。"
                    + "先用 describe_operation 查看参数。",
            annotations = @Tool.Annotations(title = "Call operation", readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false))
    String callOperation(McpConnection connection,
                         @ToolArg(name = "operation", description = "list_operations 返回的 operation") String operation,
                         @ToolArg(name = "path_params", description = "路径变量", required = false) Map<String, Object> pathParams,
                         @ToolArg(name = "query", description = "查询参数", required = false) Map<String, Object> query,
                         @ToolArg(name = "body", description = "JSON 请求体", required = false) Object body) {
        McpScope scope = context.scope(connection, List.of());
        Operation op = require(scope, operation);
        String path = fillPath(scope, op, pathParams == null ? Map.of() : pathParams);
        Map<String, Object> q = checkQuery(scope, op, query == null ? Map.of() : query);
        Object payload = null;
        if (op.bodyType() != null) {
            JsonNode node = body == null ? json.object() : json.valueToTree(body);
            checkBody(scope, op, node);
            payload = node;
        } else if (body != null) {
            throw new ToolCallException("该操作不接受请求体");
        }
        JsonNode result = gateway.call(scope, op.httpMethod(), path, q, payload);
        String text = result == null ? "{}" : json.write(result);
        return text.length() > MAX_RESULT_CHARS ? text.substring(0, MAX_RESULT_CHARS) + "…（已截断）" : text;
    }

    boolean allowed(McpScope scope, Operation op) {
        return op.anyOf() ? scope.access().allowsAny(op.permissions()) : scope.access().allowsAll(op.permissions());
    }

    private Operation require(McpScope scope, String id) {
        Operation op = id == null ? null : McpOperationCatalog.operations().get(id.trim());
        if (op == null) {
            throw new ToolCallException("未知操作：" + id + "，请先调用 list_operations");
        }
        if (!allowed(scope, op)) {
            throw new ToolCallException("无权限调用 " + op.id() + "：需要 " + String.join(op.anyOf() ? " 或 " : " 且 ",
                    op.permissions()) + "，且对应模块须在项目 MCP 设置中勾选");
        }
        return op;
    }

    String fillPath(McpScope scope, Operation op, Map<String, Object> pathParams) {
        Matcher m = VAR.matcher(op.path());
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            Object raw = pathParams.get(name);
            String value = raw == null ? null : String.valueOf(raw).trim();
            if ("projectId".equals(name)) {
                if (value == null || value.isEmpty()) {
                    value = String.valueOf(scope.projectId());
                }
                requireSameProject(scope, value);
            }
            if (value == null || value.isEmpty()) {
                throw new ToolCallException("缺少路径参数：" + name);
            }
            if (!SAFE_PATH_VALUE.matcher(value).matches()) {
                throw new ToolCallException("路径参数格式不合法：" + name);
            }
            checkOwned(scope, name, value);
            m.appendReplacement(sb, Matcher.quoteReplacement(McpGateway.encode(value)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    Map<String, Object> checkQuery(McpScope scope, Operation op, Map<String, Object> query) {
        Map<String, Object> out = new LinkedHashMap<>();
        Set<String> declared = new java.util.HashSet<>();
        for (Param p : op.params()) {
            if ("query".equals(p.in())) {
                declared.add(p.name());
            }
        }
        for (Map.Entry<String, Object> e : query.entrySet()) {
            if (!declared.contains(e.getKey())) {
                throw new ToolCallException("该操作没有查询参数：" + e.getKey());
            }
            if (e.getValue() == null) {
                continue;
            }
            checkKey(scope, e.getKey(), json.valueToTree(e.getValue()));
            out.put(e.getKey(), e.getValue());
        }
        if (declared.contains("projectId") && !out.containsKey("projectId")) {
            out.put("projectId", scope.projectId());
        }
        if (out.get("correlationId") != null) {
            checkCorrelation(scope, String.valueOf(out.getOrDefault("moduleType", "")),
                    String.valueOf(out.get("correlationId")));
        }
        return out;
    }

    void checkBody(McpScope scope, Operation op, JsonNode body) {
        if (body.isArray()) {
            body.forEach(item -> checkBody(scope, op, item));
            return;
        }
        if (!body.isObject()) {
            return;
        }
        ObjectNode obj = (ObjectNode) body;
        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> e = fields.next();
            if ("params".equals(e.getKey()) && e.getValue().isObject()) {
                e.getValue().fields().forEachRemaining(p -> checkKey(scope, p.getKey(), p.getValue()));
            } else {
                checkKey(scope, e.getKey(), e.getValue());
            }
        }
        boolean hasProjectField = op.bodyFields().stream().anyMatch(f -> "projectId".equals(f.name()));
        if (hasProjectField && !obj.hasNonNull("projectId")) {
            obj.put("projectId", scope.projectId());
        }
        if (obj.hasNonNull("correlationId")) {
            checkCorrelation(scope, obj.path("moduleType").asText(""), obj.get("correlationId").asText());
        }
    }

    private void checkCorrelation(McpScope scope, String moduleType, String correlationId) {
        long id = parseLong("correlationId", correlationId);
        if (!"defect_log".equals(moduleType)) {
            throw new ToolCallException("不支持的关联类型：" + moduleType);
        }
        objects.defectLog(scope, id);
    }

    void checkKey(McpScope scope, String key, JsonNode value) {
        if (value == null || value.isNull()) {
            return;
        }
        if ("projectId".equals(key)) {
            requireSameProject(scope, value.asText());
            return;
        }
        if (!(key.endsWith("Id") || key.endsWith("Ids") || key.endsWith("Pid"))) {
            return;
        }
        if (BENIGN_ID_KEYS.contains(key)) {
            return;
        }
        if (value.isArray()) {
            value.forEach(v -> checkOwned(scope, key, v.asText()));
        } else {
            checkOwned(scope, key, value.asText());
        }
    }

    void checkOwned(McpScope scope, String key, String rawValue) {
        if ("projectId".equals(key) || "memberId".equals(key) || "userId".equals(key) || "name".equals(key)) {
            return;
        }
        for (String part : rawValue.split(",")) {
            String v = part.trim();
            if (v.isEmpty()) {
                continue;
            }
            switch (key) {
                case "defectId", "defectIds" -> objects.defect(scope, parseLong(key, v));
                case "defectLogId", "defectLogIds" -> objects.defectLog(scope, parseLong(key, v));
                case "caseId", "caseIds" -> objects.testCase(scope, parseLong(key, v));
                case "planId", "planIds", "testPlanId" -> objects.plan(scope, v);
                case "planItemId", "planItemIds" -> objects.planItem(scope, v);
                case "moduleId", "moduleIds", "modulePid" -> {
                    if (parseLong(key, v) > 0) {
                        objects.module(scope, parseLong(key, v));
                    }
                }
                case "docId", "docIds", "docPid" -> {
                    if (parseLong(key, v) > 0) {
                        objects.document(scope, parseLong(key, v));
                    }
                }
                case "repositoryId", "repositoryIds" -> objects.repository(scope, parseLong(key, v));
                default -> throw new ToolCallException("通用通道不支持按 " + key + " 访问对象，请改用精做工具");
            }
        }
    }

    private static void requireSameProject(McpScope scope, String value) {
        if (!Objects.equals(String.valueOf(scope.projectId()), value == null ? null : value.trim())) {
            throw new ToolCallException("projectId 与目标项目不一致：通用通道只能操作授权时选择的项目");
        }
    }

    private static long parseLong(String key, String v) {
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new ToolCallException(key + " 不是合法的 ID：" + v);
        }
    }
}
