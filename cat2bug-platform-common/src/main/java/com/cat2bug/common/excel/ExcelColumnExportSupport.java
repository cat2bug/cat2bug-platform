package com.cat2bug.common.excel;

import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 根据前端表格列配置（exportColumns / exportScope）过滤 Excel 列。
 */
public final class ExcelColumnExportSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String PARAM_EXPORT_COLUMNS = "exportColumns";
    public static final String PARAM_EXPORT_SCOPE = "exportScope";
    public static final String SCOPE_DATA = "data";
    public static final String SCOPE_IMPORT_TEMPLATE = "importTemplate";

    public static final Map<String, String> DEFECT_DATA_MAP;
    public static final Map<String, String> DEFECT_TEMPLATE_MAP;
    public static final Set<String> DEFECT_TEMPLATE_REQUIRED;

    public static final Map<String, String> CASE_DATA_MAP;
    public static final Map<String, String> CASE_TEMPLATE_MAP;
    public static final Set<String> CASE_TEMPLATE_REQUIRED;
    public static final Set<String> CASE_TEMPLATE_EXCLUDED;

    static {
        Map<String, String> defectData = new LinkedHashMap<>();
        defectData.put("projectNum", "projectNum");
        defectData.put("id", "projectNum");
        defectData.put("defectTypeName", "defectType");
        defectData.put("type", "defectType");
        defectData.put("defectName", "defectName");
        defectData.put("defect.name", "defectName");
        defectData.put("defectLevel", "defectLevel");
        defectData.put("defect.level", "defectLevel");
        defectData.put("defectState", "defectState");
        defectData.put("defect.state", "defectState");
        defectData.put("moduleName", "moduleName");
        defectData.put("module", "moduleName");
        defectData.put("moduleVersion", "moduleVersion");
        defectData.put("version", "moduleVersion");
        defectData.put("defectDescribe", "defectDescribe");
        defectData.put("describe", "defectDescribe");
        defectData.put("imgUrls", "imgUrls");
        defectData.put("image", "imgUrls");
        defectData.put("annexUrls", "annexUrls");
        defectData.put("annex", "annexUrls");
        defectData.put("updateTime", "updateTime");
        defectData.put("update-time", "updateTime");
        defectData.put("planStartTime", "planStartTime");
        defectData.put("plan-start-time", "planStartTime");
        defectData.put("planEndTime", "planEndTime");
        defectData.put("plan-end-time", "planEndTime");
        defectData.put("createMember", "createMemberName");
        defectData.put("createBy", "createMemberName");
        defectData.put("handleBy", "handleByNames");
        defectData.put("handle-by", "handleByNames");
        DEFECT_DATA_MAP = Collections.unmodifiableMap(defectData);

        Map<String, String> defectTemplate = new LinkedHashMap<>();
        defectTemplate.put("defectTypeName", "defectTypeImportName");
        defectTemplate.put("type", "defectTypeImportName");
        defectTemplate.put("defectName", "defectName");
        defectTemplate.put("defect.name", "defectName");
        defectTemplate.put("defectLevel", "defectLevel");
        defectTemplate.put("defect.level", "defectLevel");
        defectTemplate.put("defectStateImportName", "defectStateImportName");
        defectTemplate.put("defect.state", "defectStateImportName");
        defectTemplate.put("moduleName", "moduleName");
        defectTemplate.put("module", "moduleName");
        defectTemplate.put("moduleVersion", "moduleVersion");
        defectTemplate.put("version", "moduleVersion");
        defectTemplate.put("defectDescribe", "defectDescribe");
        defectTemplate.put("describe", "defectDescribe");
        defectTemplate.put("imgUrls", "imgObjects");
        defectTemplate.put("image", "imgObjects");
        defectTemplate.put("handleBy", "handleByNames");
        defectTemplate.put("handle-by", "handleByNames");
        DEFECT_TEMPLATE_MAP = Collections.unmodifiableMap(defectTemplate);

        DEFECT_TEMPLATE_REQUIRED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "defectName", "handleByNames")));

        Map<String, String> caseData = new LinkedHashMap<>();
        caseData.put("caseNum", "caseNum");
        caseData.put("id", "caseNum");
        caseData.put("caseName", "caseName");
        caseData.put("case.name", "caseName");
        caseData.put("moduleName", "moduleId");
        caseData.put("module", "moduleId");
        caseData.put("caseLevel", "caseLevelName");
        caseData.put("level", "caseLevelName");
        caseData.put("casePreconditions", "casePreconditions");
        caseData.put("preconditions", "casePreconditions");
        caseData.put("caseStep", "caseStep");
        caseData.put("step", "caseStep");
        caseData.put("caseData", "caseData");
        caseData.put("data", "caseData");
        caseData.put("caseExpect", "caseExpect");
        caseData.put("expect", "caseExpect");
        caseData.put("imgUrls", "imgUrls");
        caseData.put("image", "imgUrls");
        caseData.put("annexUrls", "annexUrls");
        caseData.put("annex", "annexUrls");
        caseData.put("updateTime", "caseExportUpdateTime");
        caseData.put("update-time", "caseExportUpdateTime");
        caseData.put("defectProcessingCount", "defectProcessingCount");
        caseData.put("defect.state", "defectProcessingCount");
        CASE_DATA_MAP = Collections.unmodifiableMap(caseData);

        Map<String, String> caseTemplate = new LinkedHashMap<>();
        caseTemplate.put("caseName", "caseName");
        caseTemplate.put("case.name", "caseName");
        caseTemplate.put("moduleName", "moduleName");
        caseTemplate.put("module", "moduleName");
        caseTemplate.put("caseLevel", "caseLevel");
        caseTemplate.put("level", "caseLevel");
        caseTemplate.put("casePreconditions", "casePreconditions");
        caseTemplate.put("preconditions", "casePreconditions");
        caseTemplate.put("caseStep", "caseStepScript");
        caseTemplate.put("step", "caseStepScript");
        caseTemplate.put("caseData", "caseData");
        caseTemplate.put("data", "caseData");
        caseTemplate.put("caseExpect", "caseExpect");
        caseTemplate.put("expect", "caseExpect");
        caseTemplate.put("imgUrls", "imgObjects");
        caseTemplate.put("image", "imgObjects");
        CASE_TEMPLATE_MAP = Collections.unmodifiableMap(caseTemplate);

        CASE_TEMPLATE_REQUIRED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "caseName", "moduleName", "caseExpect")));

        CASE_TEMPLATE_EXCLUDED = Collections.unmodifiableSet(Collections.singleton("defectProcessingCount"));
    }

    private ExcelColumnExportSupport() {
    }

    public static List<String> resolveOrderedFieldNames(Map<String, Object> params, Map<String, String> propToField,
                                                        Set<String> requiredFields, Set<String> templateExcludedProps) {
        if (params == null || propToField == null) {
            return Collections.emptyList();
        }
        JsonNode columns = parseExportColumns(params.get(PARAM_EXPORT_COLUMNS));
        if (columns == null || !columns.isArray() || columns.isEmpty()) {
            return Collections.emptyList();
        }
        String scope = String.valueOf(params.getOrDefault(PARAM_EXPORT_SCOPE, SCOPE_DATA));
        if (requiredFields == null) {
            requiredFields = Collections.emptySet();
        }
        if (templateExcludedProps == null) {
            templateExcludedProps = Collections.emptySet();
        }
        List<String> orderedFieldNames = new ArrayList<>();
        for (JsonNode col : columns) {
            if (col == null || !col.isObject()) {
                continue;
            }
            String prop = textOrNull(col.get("prop"));
            String key = textOrNull(col.get("key"));
            boolean visible = col.path("visible").asBoolean(false);
            if (SCOPE_IMPORT_TEMPLATE.equals(scope)
                    && (templateExcludedProps.contains(prop) || templateExcludedProps.contains(key))) {
                continue;
            }
            String fieldName = resolveFieldName(propToField, prop, key);
            if (fieldName == null) {
                continue;
            }
            boolean include;
            if (SCOPE_IMPORT_TEMPLATE.equals(scope)) {
                include = visible || requiredFields.contains(fieldName);
            } else {
                include = visible;
            }
            if (include) {
                orderedFieldNames.add(fieldName);
            }
        }
        return orderedFieldNames;
    }

    private static String resolveFieldName(Map<String, String> propToField, String prop, String key) {
        if (StringUtils.isNotEmpty(prop) && propToField.containsKey(prop)) {
            return propToField.get(prop);
        }
        if (StringUtils.isNotEmpty(key) && propToField.containsKey(key)) {
            return propToField.get(key);
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.isEmpty(text) ? null : text;
    }

    private static JsonNode parseExportColumns(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            if (raw instanceof String str) {
                String trimmed = str.trim();
                if (StringUtils.isEmpty(trimmed)) {
                    return null;
                }
                return MAPPER.readTree(trimmed);
            }
            if (raw instanceof JsonNode node) {
                return node;
            }
            if (raw instanceof Collection<?> collection) {
                return MAPPER.valueToTree(collection);
            }
            return MAPPER.valueToTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
