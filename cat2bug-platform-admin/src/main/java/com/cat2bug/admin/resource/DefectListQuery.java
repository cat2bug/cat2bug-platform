package com.cat2bug.admin.resource;

import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.SysDefect;

import jakarta.ws.rs.core.MultivaluedMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把前端 {@code tansParams} 发出的 Spring 风格查询串绑回 {@link SysDefect}。
 * 数组写成 {@code handleBy[0]}，嵌套字段写成 {@code params[delFlag]}。
 */
public final class DefectListQuery {

    private DefectListQuery() {
    }

    public static SysDefect bind(MultivaluedMap<String, String> query) {
        SysDefect defect = new SysDefect();
        defect.setProjectId(longOrNull(first(query, "projectId")));
        defect.setProjectNum(longOrNull(first(query, "projectNum")));
        defect.setDefectName(blankToNull(first(query, "defectName")));
        defect.setNameVersionKeyword(blankToNull(first(query, "nameVersionKeyword")));
        defect.setModuleVersion(blankToNull(first(query, "moduleVersion")));
        defect.setDefectLevel(blankToNull(first(query, "defectLevel")));
        defect.setCreateBy(blankToNull(first(query, "createBy")));
        defect.setUpdateBy(blankToNull(first(query, "updateBy")));
        defect.setModuleId(longOrNull(first(query, "moduleId")));
        defect.setCaseId(longOrNull(first(query, "caseId")));
        defect.setTestPlanId(longOrNull(first(query, "testPlanId")));
        defect.setDefectState(DefectResource.enumParam(DefectConstants::parseState, first(query, "defectState")));
        defect.setDefectType(DefectResource.enumParam(DefectConstants::parseType, first(query, "defectType")));
        List<Long> handleBy = indexedLongs(query, "handleBy");
        if (!handleBy.isEmpty()) {
            defect.setHandleBy(handleBy);
        }
        List<Long> createByIds = indexedLongs(query, "createByIds");

        Map<String, Object> params = new LinkedHashMap<>();
        putText(params, "delFlag", first(query, "params[delFlag]"));
        putText(params, "nameVersionKeyword", first(query, "params[nameVersionKeyword]"));
        putText(params, "search", first(query, "params[search]"));
        putText(params, "beginUpdateTime", first(query, "params[beginUpdateTime]"));
        putText(params, "endUpdateTime", first(query, "params[endUpdateTime]"));
        putText(params, "beginCreateTime", first(query, "params[beginCreateTime]"));
        putText(params, "endCreateTime", first(query, "params[endCreateTime]"));
        putText(params, "beginHandleTime", first(query, "params[beginHandleTime]"));
        putText(params, "endHandleTime", first(query, "params[endHandleTime]"));
        putText(params, "calendarStartDate", first(query, "params[calendarStartDate]"));
        putText(params, "calendarEndDate", first(query, "params[calendarEndDate]"));
        putText(params, "defectIds", first(query, "params[defectIds]"));
        Long userId = longOrNull(first(query, "params[userId]"));
        if (userId != null) {
            params.put("userId", userId);
        }
        Long handleById = longOrNull(first(query, "handleById"));
        if (handleById != null) {
            params.put("handleById", handleById);
        }
        if (defect.getProjectNum() != null) {
            params.put("projectNum", defect.getProjectNum());
        }
        String states = first(query, "params[defectStates]");
        if (states == null) {
            states = joinIndexed(query, "params[defectStates]");
        }
        List<Integer> parsedStates = parseStates(states);
        if (!parsedStates.isEmpty()) {
            params.put("defectStates", parsedStates);
        }
        if (!createByIds.isEmpty()) {
            params.put("createByIds", createByIds);
        }
        defect.setParams(params);
        return defect;
    }

    public static Integer pageNum(MultivaluedMap<String, String> query) {
        return intOrNull(first(query, "pageNum"));
    }

    public static Integer pageSize(MultivaluedMap<String, String> query) {
        return intOrNull(first(query, "pageSize"));
    }

    static List<Integer> parseStates(String value) {
        List<Integer> states = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return states;
        }
        for (String part : value.split(",")) {
            Integer state = DefectResource.enumParam(DefectConstants::parseState, part.trim());
            if (state != null && !states.contains(state)) {
                states.add(state);
            }
        }
        return states;
    }

    private static void putText(Map<String, Object> params, String key, String value) {
        String text = blankToNull(value);
        if (text != null) {
            params.put(key, text);
        }
    }

    private static String first(MultivaluedMap<String, String> query, String name) {
        if (query == null) {
            return null;
        }
        return query.getFirst(name);
    }

    public static List<Long> indexedLongs(MultivaluedMap<String, String> query, String name) {
        List<Long> values = new ArrayList<>();
        if (query == null) {
            return values;
        }
        String plain = longText(first(query, name));
        if (plain != null) {
            for (String part : plain.split(",")) {
                Long value = longOrNull(part.trim());
                if (value != null) {
                    values.add(value);
                }
            }
        }
        String prefix = name + "[";
        List<String> keys = new ArrayList<>(query.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            if (key.startsWith(prefix) && key.endsWith("]")) {
                Long value = longOrNull(first(query, key));
                if (value != null && !values.contains(value)) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static String joinIndexed(MultivaluedMap<String, String> query, String name) {
        if (query == null) {
            return null;
        }
        String prefix = name + "[";
        List<String> parts = new ArrayList<>();
        List<String> keys = new ArrayList<>(query.keySet());
        keys.sort(String::compareTo);
        for (String key : keys) {
            if (key.startsWith(prefix) && key.endsWith("]")) {
                String value = blankToNull(first(query, key));
                if (value != null) {
                    parts.add(value);
                }
            }
        }
        return parts.isEmpty() ? null : String.join(",", parts);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String longText(String value) {
        return blankToNull(value);
    }

    private static Long longOrNull(String value) {
        String text = blankToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer intOrNull(String value) {
        Long number = longOrNull(value);
        return number == null ? null : number.intValue();
    }
}
