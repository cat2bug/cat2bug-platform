package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.SysCase;
import com.cat2bug.common.domain.SysCaseStep;
import com.cat2bug.common.domain.SysPlanItem;
import com.fasterxml.jackson.databind.JsonNode;
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

import static com.cat2bug.admin.mcp.McpDefectTools.blankToNull;
import static com.cat2bug.admin.mcp.McpDefectTools.clampPageSize;

/**
 * 用例、测试计划、交付物、报告、文档、仪表盘工具。
 */
public class McpWorkTools {

    public record CaseStepArg(String describe, String expect) {
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

    // ---------------------------------------------------------------- 用例

    @Tool(name = "search_cases",
            description = "查询测试用例列表。",
            annotations = @Tool.Annotations(title = "Search cases", readOnlyHint = true, openWorldHint = false))
    String searchCases(McpConnection connection,
                       @ToolArg(name = "keyword", description = "按用例标题模糊匹配", required = false) String keyword,
                       @ToolArg(name = "module_id", description = "交付物（模块）ID", required = false) Long moduleId,
                       @ToolArg(name = "case_num", description = "项目内用例编号", required = false) Long caseNum,
                       @ToolArg(name = "case_type_id", description = "用例类型 ID", required = false) Long caseTypeId,
                       @ToolArg(name = "review_status", description = "评审筛选：none 未评审、in_review 评审中、passed 通过、changes 需修改", required = false) String reviewStatus,
                       @ToolArg(name = "page", description = "页码", required = false) Integer page,
                       @ToolArg(name = "page_size", description = "每页条数，默认 20，最大 100", required = false) Integer pageSize) {
        McpScope scope = context.scope(connection, "system:case:list");
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("projectId", scope.projectId());
        q.put("caseName", blankToNull(keyword));
        q.put("moduleId", moduleId);
        q.put("caseNum", caseNum);
        q.put("caseType", caseTypeId);
        q.put("reviewStatus", blankToNull(reviewStatus));
        q.put("pageNum", page == null || page < 1 ? 1 : page);
        q.put("pageSize", clampPageSize(pageSize));
        return json.write(json.page(gateway.get(scope, "/system/case/list", q), json::caseSummary));
    }

    @Tool(name = "get_case",
            description = "读取测试用例详情（前置条件、步骤、预期、数据）。",
            annotations = @Tool.Annotations(title = "Get case", readOnlyHint = true, openWorldHint = false))
    String getCase(McpConnection connection,
                   @ToolArg(name = "case_id", description = "用例 ID") Long caseId) {
        McpScope scope = context.scope(connection, "system:case:query");
        objects.testCase(scope, caseId);
        return json.write(json.caseDetail(McpJson.data(gateway.get(scope, "/system/case/" + caseId, null))));
    }

    @Tool(name = "create_case",
            description = "新建测试用例。steps 为步骤列表（describe 操作、expect 期望）。建议传 idempotency_key。",
            annotations = @Tool.Annotations(title = "Create case", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String createCase(McpConnection connection,
                      @ToolArg(name = "name", description = "用例标题") String name,
                      @ToolArg(name = "module_id", description = "交付物（模块）ID") Long moduleId,
                      @ToolArg(name = "preconditions", description = "前置条件", required = false) String preconditions,
                      @ToolArg(name = "expect", description = "整体预期结果", required = false) String expect,
                      @ToolArg(name = "steps", description = "步骤列表", required = false) List<CaseStepArg> steps,
                      @ToolArg(name = "level", description = "用例等级 1～5，默认 2", required = false) Long level,
                      @ToolArg(name = "case_data", description = "测试数据", required = false) String caseData,
                      @ToolArg(name = "case_type_id", description = "用例类型 ID，可空", required = false) Long caseTypeId,
                      @ToolArg(name = "idempotency_key", description = "幂等键", required = false) String idempotencyKey) {
        McpScope scope = context.scope(connection, "system:case:add");
        if (name == null || name.isBlank()) {
            throw new ToolCallException("用例标题不能为空");
        }
        objects.module(scope, moduleId);
        McpIdempotency.Outcome outcome = idempotency.run(scope.userId(), "create_case", idempotencyKey, () -> {
            SysCase body = new SysCase();
            body.setProjectId(scope.projectId());
            body.setCaseName(name.trim());
            body.setModuleId(moduleId);
            body.setCasePreconditions(preconditions);
            body.setCaseExpect(expect);
            body.setCaseStep(toSteps(steps));
            body.setCaseLevel(level == null ? 2L : level);
            body.setCaseData(caseData);
            if (caseTypeId != null) {
                body.setCaseType(caseTypeId);
                body.setUpdateCaseType(true);
            }
            JsonNode data = McpJson.data(gateway.post(scope, "/system/case", body));
            if (data == null || !data.hasNonNull("caseId")) {
                throw new ToolCallException("新建用例失败：接口未返回用例 ID");
            }
            return data.get("caseId").asText();
        });
        ObjectNode out = json.caseDetail(json.valueToTree(objects.testCase(scope, Long.valueOf(outcome.resultId()))));
        out.put("replayed", outcome.replayed());
        return json.write(out);
    }

    @Tool(name = "update_case",
            description = "修改测试用例，只传需要改的字段；steps 传入时整体替换原步骤。",
            annotations = @Tool.Annotations(title = "Update case", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String updateCase(McpConnection connection,
                      @ToolArg(name = "case_id", description = "用例 ID") Long caseId,
                      @ToolArg(name = "name", description = "用例标题", required = false) String name,
                      @ToolArg(name = "module_id", description = "交付物（模块）ID", required = false) Long moduleId,
                      @ToolArg(name = "preconditions", description = "前置条件", required = false) String preconditions,
                      @ToolArg(name = "expect", description = "整体预期结果", required = false) String expect,
                      @ToolArg(name = "steps", description = "步骤列表", required = false) List<CaseStepArg> steps,
                      @ToolArg(name = "level", description = "用例等级", required = false) Long level,
                      @ToolArg(name = "case_data", description = "测试数据", required = false) String caseData,
                      @ToolArg(name = "case_type_id", description = "用例类型 ID", required = false) Long caseTypeId,
                      @ToolArg(name = "clear_case_type", description = "为 true 时清空类型", required = false) Boolean clearCaseType) {
        McpScope scope = context.scope(connection, "system:case:edit");
        SysCase existing = objects.testCase(scope, caseId);
        if (moduleId != null) {
            objects.module(scope, moduleId);
        }
        if (name != null && !name.isBlank()) {
            existing.setCaseName(name.trim());
        }
        if (moduleId != null) {
            existing.setModuleId(moduleId);
        }
        if (preconditions != null) {
            existing.setCasePreconditions(preconditions);
        }
        if (expect != null) {
            existing.setCaseExpect(expect);
        }
        if (steps != null) {
            existing.setCaseStep(toSteps(steps));
        }
        if (level != null) {
            existing.setCaseLevel(level);
        }
        if (caseData != null) {
            existing.setCaseData(caseData);
        }
        if (Boolean.TRUE.equals(clearCaseType)) {
            existing.setCaseType(null);
            existing.setUpdateCaseType(true);
        } else if (caseTypeId != null) {
            existing.setCaseType(caseTypeId);
            existing.setUpdateCaseType(true);
        }
        existing.setProjectId(scope.projectId());
        gateway.put(scope, "/system/case", existing);
        return json.write(json.caseDetail(json.valueToTree(objects.testCase(scope, caseId))));
    }

    @Tool(name = "list_case_types",
            description = "列出当前项目的用例类型。",
            annotations = @Tool.Annotations(title = "List case types", readOnlyHint = true, openWorldHint = false))
    String listCaseTypes(McpConnection connection) {
        McpScope scope = context.scope(connection, "system:case:list");
        return json.write(McpJson.data(gateway.get(scope, "/system/project/" + scope.projectId() + "/case-types", null)));
    }

    @Tool(name = "create_case_type",
            description = "新增项目用例类型。建议传 idempotency_key。",
            annotations = @Tool.Annotations(title = "Create case type", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String createCaseType(McpConnection connection,
                          @ToolArg(name = "name", description = "类型名称") String name,
                          @ToolArg(name = "idempotency_key", description = "幂等键", required = false) String idempotencyKey) {
        McpScope scope = context.scope(connection, "system:project:case-type:add");
        if (name == null || name.isBlank()) {
            throw new ToolCallException("类型名称不能为空");
        }
        McpIdempotency.Outcome outcome = idempotency.run(scope.userId(), "create_case_type", idempotencyKey, () -> {
            JsonNode data = McpJson.data(gateway.post(scope, "/system/project/" + scope.projectId() + "/case-types",
                    Map.of("typeName", name.trim())));
            if (data == null || !data.hasNonNull("typeId")) {
                throw new ToolCallException("新增类型失败");
            }
            return data.get("typeId").asText();
        });
        ObjectNode out = json.object();
        out.put("typeId", Long.valueOf(outcome.resultId()));
        out.put("replayed", outcome.replayed());
        return json.write(out);
    }

    @Tool(name = "update_case_type",
            description = "重命名项目用例类型。",
            annotations = @Tool.Annotations(title = "Update case type", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String updateCaseType(McpConnection connection,
                          @ToolArg(name = "type_id", description = "类型 ID") Long typeId,
                          @ToolArg(name = "name", description = "新名称") String name) {
        McpScope scope = context.scope(connection, "system:project:case-type:edit");
        gateway.put(scope, "/system/project/" + scope.projectId() + "/case-types/" + typeId, Map.of("typeName", name == null ? "" : name.trim()));
        return json.write(json.object().put("typeId", typeId));
    }

    @Tool(name = "delete_case_type",
            description = "删除项目用例类型。仍被用例使用时会失败并说明条数。",
            annotations = @Tool.Annotations(title = "Delete case type", readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    String deleteCaseType(McpConnection connection,
                          @ToolArg(name = "type_id", description = "类型 ID") Long typeId) {
        McpScope scope = context.scope(connection, "system:project:case-type:remove");
        gateway.call(scope, "DELETE", "/system/project/" + scope.projectId() + "/case-types/" + typeId, null, null);
        return json.write(json.object().put("typeId", typeId));
    }

    @Tool(name = "start_case_review",
            description = "对勾选的用例发起一次评审。其中任一用例已在进行中的评审里时整批失败。建议传 idempotency_key。",
            annotations = @Tool.Annotations(title = "Start case review", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String startCaseReview(McpConnection connection,
                           @ToolArg(name = "case_ids", description = "用例 ID 列表") List<Long> caseIds,
                           @ToolArg(name = "idempotency_key", description = "幂等键", required = false) String idempotencyKey) {
        McpScope scope = context.scope(connection, "system:case:review");
        McpIdempotency.Outcome outcome = idempotency.run(scope.userId(), "start_case_review", idempotencyKey, () -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("projectId", scope.projectId());
            body.put("caseIds", caseIds);
            JsonNode data = McpJson.data(gateway.post(scope, "/system/case-review", body));
            if (data == null || !data.hasNonNull("reviewId")) {
                throw new ToolCallException("发起评审失败");
            }
            return data.get("reviewId").asText();
        });
        ObjectNode out = reviewNode(scope, Long.valueOf(outcome.resultId()));
        out.put("replayed", outcome.replayed());
        return json.write(out);
    }

    @Tool(name = "comment_case_review",
            description = "给评审中的一条用例填写结论：pending 待定、passed 通过、changes 需修改。",
            annotations = @Tool.Annotations(title = "Comment case review", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String commentCaseReview(McpConnection connection,
                             @ToolArg(name = "review_id", description = "评审 ID") Long reviewId,
                             @ToolArg(name = "case_id", description = "用例 ID") Long caseId,
                             @ToolArg(name = "verdict", description = "pending / passed / changes") String verdict,
                             @ToolArg(name = "comment", description = "评语", required = false) String comment) {
        McpScope scope = context.scope(connection, "system:case:review");
        requireReviewInProject(scope, reviewId);
        gateway.put(scope, "/system/case-review/" + reviewId + "/item", Map.of(
                "caseId", caseId,
                "verdict", verdict == null ? "" : verdict,
                "comment", comment == null ? "" : comment));
        return json.write(reviewNode(scope, reviewId));
    }

    @Tool(name = "complete_case_review",
            description = "完成评审：每条都须为通过或需修改，然后生成 Markdown 纪要并写回用例上的评审标记。",
            annotations = @Tool.Annotations(title = "Complete case review", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String completeCaseReview(McpConnection connection,
                              @ToolArg(name = "review_id", description = "评审 ID") Long reviewId) {
        McpScope scope = context.scope(connection, "system:case:review");
        requireReviewInProject(scope, reviewId);
        gateway.post(scope, "/system/case-review/" + reviewId + "/complete", Map.of());
        return json.write(reviewNode(scope, reviewId));
    }

    @Tool(name = "update_case_review_report",
            description = "修改已完成评审的纪要正文，不改变结论和用例上的评审标记。",
            annotations = @Tool.Annotations(title = "Update case review report", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String updateCaseReviewReport(McpConnection connection,
                                  @ToolArg(name = "review_id", description = "评审 ID") Long reviewId,
                                  @ToolArg(name = "report_markdown", description = "纪要 Markdown") String reportMarkdown) {
        McpScope scope = context.scope(connection, "system:case:review");
        requireReviewInProject(scope, reviewId);
        gateway.put(scope, "/system/case-review/" + reviewId + "/report", Map.of("reportMarkdown", reportMarkdown == null ? "" : reportMarkdown));
        return json.write(reviewNode(scope, reviewId));
    }

    @Tool(name = "list_case_reviews",
            description = "列出当前项目的用例评审。",
            annotations = @Tool.Annotations(title = "List case reviews", readOnlyHint = true, openWorldHint = false))
    String listCaseReviews(McpConnection connection) {
        McpScope scope = context.scope(connection, "system:case:query");
        return json.write(McpJson.data(gateway.get(scope, "/system/case-review", Map.of("projectId", scope.projectId()))));
    }

    @Tool(name = "get_case_review",
            description = "读取一次用例评审及其条目和纪要。",
            annotations = @Tool.Annotations(title = "Get case review", readOnlyHint = true, openWorldHint = false))
    String getCaseReview(McpConnection connection,
                         @ToolArg(name = "review_id", description = "评审 ID") Long reviewId) {
        McpScope scope = context.scope(connection, "system:case:query");
        return json.write(reviewNode(scope, reviewId));
    }

    private ObjectNode reviewNode(McpScope scope, Long reviewId) {
        JsonNode data = requireReviewInProject(scope, reviewId);
        return (ObjectNode) data;
    }

    private JsonNode requireReviewInProject(McpScope scope, Long reviewId) {
        JsonNode data = McpJson.data(gateway.get(scope, "/system/case-review/" + reviewId, null));
        if (data == null || data.path("projectId").asLong() != scope.projectId()) {
            throw new ToolCallException("评审不存在");
        }
        return data;
    }

    // ---------------------------------------------------------------- 测试计划

    @Tool(name = "list_plans",
            description = "查询测试计划列表（含通过 / 失败 / 未执行数量与缺陷统计）。",
            annotations = @Tool.Annotations(title = "List plans", readOnlyHint = true, openWorldHint = false))
    String listPlans(McpConnection connection,
                     @ToolArg(name = "keyword", description = "按计划名称模糊匹配", required = false) String keyword,
                     @ToolArg(name = "page", description = "页码", required = false) Integer page,
                     @ToolArg(name = "page_size", description = "每页条数", required = false) Integer pageSize) {
        McpScope scope = context.scope(connection, "system:plan:list");
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("projectId", scope.projectId());
        q.put("planName", blankToNull(keyword));
        q.put("pageNum", page == null || page < 1 ? 1 : page);
        q.put("pageSize", clampPageSize(pageSize));
        return json.write(json.page(gateway.get(scope, "/system/plan/list", q), p -> {
            ObjectNode o = json.object();
            McpJson.copy(p, o, "planId", "planNumber", "planName", "planVersion", "planStartTime", "planEndTime",
                    "itemTotal", "passCount", "failCount", "unexecutedCount", "defectCount", "remark");
            return o;
        }));
    }

    @Tool(name = "list_plan_items",
            description = "列出测试计划的子项（每个子项对应一个用例）。state 取值 pass / not_pass / unexecuted。",
            annotations = @Tool.Annotations(title = "List plan items", readOnlyHint = true, openWorldHint = false))
    String listPlanItems(McpConnection connection,
                         @ToolArg(name = "plan_id", description = "测试计划 ID") String planId,
                         @ToolArg(name = "state", description = "按执行状态过滤", required = false) String state,
                         @ToolArg(name = "page", description = "页码", required = false) Integer page,
                         @ToolArg(name = "page_size", description = "每页条数", required = false) Integer pageSize) {
        McpScope scope = context.scope(connection, "system:plan:run", "system:plan:edit");
        objects.plan(scope, planId);
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("planId", planId);
        q.put("projectId", scope.projectId());
        q.put("planItemState", normalizeItemState(state));
        q.put("pageNum", page == null || page < 1 ? 1 : page);
        q.put("pageSize", clampPageSize(pageSize));
        return json.write(json.page(gateway.get(scope, "/system/PlanItem/list", q), i -> {
            ObjectNode o = json.object();
            McpJson.copy(i, o, "planItemId", "caseId", "caseNum", "caseName", "moduleName", "planItemState",
                    "defectIds", "updateTime");
            return o;
        }));
    }

    @Tool(name = "update_plan_item",
            description = "记录测试计划子项的执行结果（pass / not_pass / unexecuted），可同时关联一个缺陷。",
            annotations = @Tool.Annotations(title = "Update plan item", readOnlyHint = false, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    String updatePlanItem(McpConnection connection,
                          @ToolArg(name = "plan_item_id", description = "测试计划子项 ID") String planItemId,
                          @ToolArg(name = "state", description = "pass / not_pass / unexecuted") String state,
                          @ToolArg(name = "defect_id", description = "关联的缺陷 ID", required = false) Long defectId) {
        McpScope scope = context.scope(connection, "system:plan:run", "system:plan:edit");
        SysPlanItem item = objects.planItem(scope, planItemId);
        String s = normalizeItemState(state);
        if (s == null) {
            throw new ToolCallException("state 不能为空");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("planItemId", item.getPlanItemId());
        body.put("planId", item.getPlanId());
        body.put("planItemState", s);
        if (defectId != null) {
            objects.defect(scope, defectId);
            body.put("params", Map.of("defectId", defectId));
        }
        gateway.put(scope, "/system/PlanItem", body);
        ObjectNode out = json.object();
        out.put("planItemId", item.getPlanItemId());
        out.put("planItemState", s);
        return json.write(out);
    }

    // ---------------------------------------------------------------- 交付物

    @Tool(name = "list_modules",
            description = "列出项目的交付物（模块）树，缺陷与用例按交付物归类。",
            annotations = @Tool.Annotations(title = "List modules", readOnlyHint = true, openWorldHint = false))
    String listModules(McpConnection connection,
                       @ToolArg(name = "keyword", description = "按名称过滤", required = false) String keyword) {
        McpScope scope = context.scope(connection, "system:module:list", "system:defect:list");
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("projectId", scope.projectId());
        q.put("moduleName", blankToNull(keyword));
        return json.write(McpJson.data(gateway.get(scope, "/system/module/tree", q)));
    }

    // ---------------------------------------------------------------- 文档

    @Tool(name = "list_documents",
            description = "查询项目文档（目录与文件）。parent_id 为空时列根目录。",
            annotations = @Tool.Annotations(title = "List documents", readOnlyHint = true, openWorldHint = false))
    String listDocuments(McpConnection connection,
                         @ToolArg(name = "keyword", description = "按名称模糊匹配", required = false) String keyword,
                         @ToolArg(name = "parent_id", description = "父目录 ID", required = false) Long parentId,
                         @ToolArg(name = "page", description = "页码", required = false) Integer page,
                         @ToolArg(name = "page_size", description = "每页条数", required = false) Integer pageSize) {
        McpScope scope = context.scope(connection, "system:document:list");
        if (parentId != null) {
            objects.document(scope, parentId);
        }
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("projectId", scope.projectId());
        q.put("docName", blankToNull(keyword));
        q.put("docPid", parentId);
        q.put("pageNum", page == null || page < 1 ? 1 : page);
        q.put("pageSize", clampPageSize(pageSize));
        return json.write(json.page(gateway.get(scope, "/system/document/list", q), d -> {
            ObjectNode o = json.object();
            McpJson.copy(d, o, "docId", "docPid", "docName", "docType", "fileExtension", "fileVersion", "leaf",
                    "fileUrl", "updateTime", "updateBy", "docRemakr");
            return o;
        }));
    }

    // ---------------------------------------------------------------- 仪表盘

    @Tool(name = "get_project_stats",
            description = "项目概况统计：缺陷（按状态 / 类型）、用例、交付物、成员数量等。",
            annotations = @Tool.Annotations(title = "Get project stats", readOnlyHint = true, openWorldHint = false))
    String getProjectStats(McpConnection connection) {
        McpScope scope = context.scope(connection, "system:dashboard:query");
        String base = "/system/dashboard/" + scope.projectId();
        ObjectNode out = json.object();
        out.set("defect", McpJson.data(gateway.get(scope, base + "/defect", null)));
        out.set("case", McpJson.data(gateway.get(scope, base + "/case", null)));
        out.set("module", McpJson.data(gateway.get(scope, base + "/module", null)));
        out.set("member", McpJson.data(gateway.get(scope, base + "/member", null)));
        return json.write(out);
    }

    static String normalizeItemState(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        String s = state.trim().toLowerCase(java.util.Locale.ROOT);
        if (!List.of("pass", "not_pass", "unexecuted").contains(s)) {
            throw new ToolCallException("state 取值应为 pass / not_pass / unexecuted：" + state);
        }
        return s;
    }

    private static List<SysCaseStep> toSteps(List<CaseStepArg> steps) {
        List<SysCaseStep> out = new ArrayList<>();
        if (steps == null) {
            return out;
        }
        for (CaseStepArg s : steps) {
            if (s == null) {
                continue;
            }
            SysCaseStep step = new SysCaseStep();
            step.setStepDescribe(s.describe());
            step.setStepExpect(s.expect());
            out.add(step);
        }
        return out;
    }
}
