package com.cat2bug.admin.mcp;

import com.cat2bug.common.constant.DefectConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkiverse.mcp.server.ToolCallException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Locale;

/**
 * 工具输出：把网页接口返回的 JSON 精简成对 AI 友好的结构（去掉头像、焦点成员等噪声，状态/类型给出名称）。
 */
@ApplicationScoped
public class McpJson {

    static final int MAX_TEXT = 20_000;

    @Inject
    ObjectMapper objectMapper;

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ToolCallException("结果序列化失败：" + e.getMessage());
        }
    }

    public ObjectNode object() {
        return objectMapper.createObjectNode();
    }

    public ArrayNode array() {
        return objectMapper.createArrayNode();
    }

    public JsonNode valueToTree(Object value) {
        return objectMapper.valueToTree(value);
    }

    /** AjaxResult.data */
    public static JsonNode data(JsonNode ajax) {
        return ajax == null ? null : ajax.get("data");
    }

    /** TableDataInfo → {total, rows:[mapped]} */
    public ObjectNode page(JsonNode table, java.util.function.Function<JsonNode, JsonNode> mapper) {
        ObjectNode out = object();
        out.put("total", table != null && table.has("total") ? table.get("total").asLong() : 0);
        ArrayNode rows = out.putArray("rows");
        if (table != null && table.get("rows") instanceof ArrayNode arr) {
            arr.forEach(r -> rows.add(mapper.apply(r)));
        }
        return out;
    }

    public ObjectNode defectSummary(JsonNode d) {
        ObjectNode o = object();
        copy(d, o, "defectId", "projectNum", "defectName", "defectLevel", "moduleId", "moduleName",
                "moduleVersion", "createTime", "updateTime", "handleTime", "planStartTime", "planEndTime");
        o.put("defectType", typeName(d.path("defectType")));
        o.put("defectState", stateName(d.path("defectState")));
        o.set("handleBy", members(d.path("handleByList"), d.path("handleBy")));
        if (d.hasNonNull("createMember")) {
            o.set("createBy", member(d.get("createMember")));
        } else if (d.hasNonNull("createById")) {
            o.put("createById", d.get("createById").asLong());
        }
        return o;
    }

    public ObjectNode defectDetail(JsonNode d) {
        ObjectNode o = defectSummary(d);
        copy(d, o, "caseId", "testPlanId", "imgUrls", "annexUrls", "customFields");
        o.put("defectDescribe", clip(d.path("defectDescribe").asText(null)));
        return o;
    }

    public ObjectNode defectLog(JsonNode l) {
        ObjectNode o = object();
        copy(l, o, "defectLogId", "defectLogType", "createTime", "createByName", "receiveBy", "annexUrls",
                "operChannel", "clientName");
        o.put("defectLogDescribe", clip(l.path("defectLogDescribe").asText(null)));
        if (l.get("commits") instanceof ArrayNode commits && !commits.isEmpty()) {
            ArrayNode out = o.putArray("commits");
            commits.forEach(c -> {
                ObjectNode co = object();
                copy(c, co, "repositoryId", "repositoryName", "commitSha", "commitUrl", "commitMessage", "commitTime");
                out.add(co);
            });
        }
        if (l.get("commentList") instanceof ArrayNode comments && !comments.isEmpty()) {
            ArrayNode out = o.putArray("comments");
            comments.forEach(c -> {
                ObjectNode co = object();
                copy(c, co, "commentId", "commentContent", "createTime");
                if (c.hasNonNull("createMember")) {
                    co.put("createBy", c.get("createMember").path("nickName").asText(null));
                }
                out.add(co);
            });
        }
        return o;
    }

    public ObjectNode caseSummary(JsonNode c) {
        ObjectNode o = object();
        copy(c, o, "caseId", "caseNum", "caseName", "moduleId", "moduleName", "caseType", "caseTypeName",
                "reviewStatus", "caseLevel", "updateTime");
        return o;
    }

    public ObjectNode caseDetail(JsonNode c) {
        ObjectNode o = caseSummary(c);
        copy(c, o, "casePreconditions", "caseExpect", "caseStep", "caseData", "remark", "imgUrls", "annexUrls",
                "defectProcessingCount", "defectAuditCount", "defectCloseCount");
        return o;
    }

    public ArrayNode members(JsonNode list, JsonNode ids) {
        ArrayNode arr = array();
        if (list instanceof ArrayNode users && !users.isEmpty()) {
            users.forEach(u -> arr.add(member(u)));
        } else if (ids instanceof ArrayNode idArr) {
            idArr.forEach(id -> arr.add(object().put("userId", id.asLong())));
        }
        return arr;
    }

    public ObjectNode member(JsonNode u) {
        ObjectNode o = object();
        copy(u, o, "userId", "userName", "nickName");
        return o;
    }

    public static void copy(JsonNode from, ObjectNode to, String... fields) {
        if (from == null) {
            return;
        }
        for (String f : fields) {
            JsonNode v = from.get(f);
            if (v != null && !v.isNull() && !(v.isTextual() && v.asText().isEmpty())) {
                to.set(f, v);
            }
        }
    }

    public static String typeName(JsonNode v) {
        if (v == null || v.isNull() || v.isMissingNode()) {
            return null;
        }
        if (v.isTextual() && !v.asText().chars().allMatch(Character::isDigit)) {
            return v.asText();
        }
        int i = v.asInt(-1);
        return i >= 0 && i < DefectConstants.DEFECT_TYPES.length ? DefectConstants.DEFECT_TYPES[i] : v.asText();
    }

    public static String stateName(JsonNode v) {
        if (v == null || v.isNull() || v.isMissingNode()) {
            return null;
        }
        if (v.isTextual() && !v.asText().chars().allMatch(Character::isDigit)) {
            return v.asText();
        }
        int i = v.asInt(-1);
        return i >= 0 && i < DefectConstants.DEFECT_STATES.length ? DefectConstants.DEFECT_STATES[i] : v.asText();
    }

    public static Integer typeCode(String name) {
        return indexOf(DefectConstants.DEFECT_TYPES, name, "缺陷类型", "BUG / TASK / DEMAND");
    }

    public static Integer stateCode(String name) {
        return indexOf(DefectConstants.DEFECT_STATES, name, "缺陷状态", "PROCESSING / AUDIT / REJECTED / CLOSED");
    }

    private static Integer indexOf(String[] names, String name, String kind, String hint) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String n = name.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(n)) {
                return i;
            }
        }
        throw new ToolCallException(kind + "取值应为 " + hint + "：" + name);
    }

    public static String clip(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > MAX_TEXT ? s.substring(0, MAX_TEXT) + "…（已截断）" : s;
    }
}
