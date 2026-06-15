package com.cat2bug.common.utils.poi;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.utils.StringUtils;

import java.util.*;

/**
 * 根据前端表格列配置（exportColumns / exportScope）过滤 Excel 导出列。
 */
public final class ExcelColumnExportSupport
{
    public static final String PARAM_EXPORT_COLUMNS = ExcelExportColumnParams.PARAM_EXPORT_COLUMNS;
    public static final String PARAM_EXPORT_SCOPE = ExcelExportColumnParams.PARAM_EXPORT_SCOPE;
    public static final String SCOPE_DATA = ExcelExportColumnParams.SCOPE_DATA;
    public static final String SCOPE_IMPORT_TEMPLATE = ExcelExportColumnParams.SCOPE_IMPORT_TEMPLATE;

    public static final Map<String, String> DEFECT_DATA_MAP;
    public static final Map<String, String> DEFECT_TEMPLATE_MAP;
    public static final Set<String> DEFECT_TEMPLATE_REQUIRED;
    public static final Map<String, String> CASE_DATA_MAP;
    public static final Map<String, String> CASE_TEMPLATE_MAP;
    public static final Set<String> CASE_TEMPLATE_REQUIRED;
    public static final Set<String> CASE_TEMPLATE_EXCLUDED;

    static
    {
        Map<String, String> defectData = new LinkedHashMap<>();
        defectData.put("projectNum", "projectNum");
        defectData.put("id", "projectNum");
        defectData.put("defectTypeName", "defectType");
        defectData.put("defectName", "defectName");
        defectData.put("defectLevel", "defectLevel");
        defectData.put("defectState", "defectState");
        defectData.put("moduleName", "moduleName");
        defectData.put("moduleVersion", "moduleVersion");
        defectData.put("defectDescribe", "defectDescribe");
        defectData.put("imgUrls", "imgUrls");
        defectData.put("annexUrls", "annexUrls");
        defectData.put("updateTime", "updateTime");
        defectData.put("planStartTime", "planStartTime");
        defectData.put("planEndTime", "planEndTime");
        defectData.put("createMember", "createMemberName");
        defectData.put("handleBy", "handleByNames");
        DEFECT_DATA_MAP = Collections.unmodifiableMap(defectData);

        Map<String, String> defectTemplate = new LinkedHashMap<>();
        defectTemplate.put("defectTypeName", "defectTypeImportName");
        defectTemplate.put("defectName", "defectName");
        defectTemplate.put("defectLevel", "defectLevel");
        defectTemplate.put("defectState", "defectStateImportName");
        defectTemplate.put("moduleName", "moduleName");
        defectTemplate.put("moduleVersion", "moduleVersion");
        defectTemplate.put("defectDescribe", "defectDescribe");
        defectTemplate.put("imgUrls", "imgObjects");
        defectTemplate.put("handleBy", "handleByNames");
        DEFECT_TEMPLATE_MAP = Collections.unmodifiableMap(defectTemplate);

        DEFECT_TEMPLATE_REQUIRED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "defectName", "handleByNames")));

        Map<String, String> caseData = new LinkedHashMap<>();
        caseData.put("caseNum", "caseNum");
        caseData.put("caseName", "caseName");
        caseData.put("moduleName", "moduleId");
        caseData.put("caseLevel", "caseLevelName");
        caseData.put("casePreconditions", "casePreconditions");
        caseData.put("caseStep", "caseStep");
        caseData.put("caseData", "caseData");
        caseData.put("caseExpect", "caseExpect");
        caseData.put("imgUrls", "imgUrls");
        caseData.put("annexUrls", "annexUrls");
        caseData.put("updateTime", "caseExportUpdateTime");
        caseData.put("defectProcessingCount", "defectProcessingCount");
        CASE_DATA_MAP = Collections.unmodifiableMap(caseData);

        Map<String, String> caseTemplate = new LinkedHashMap<>();
        caseTemplate.put("caseName", "caseName");
        caseTemplate.put("moduleName", "moduleName");
        caseTemplate.put("caseLevel", "caseLevel");
        caseTemplate.put("casePreconditions", "casePreconditions");
        caseTemplate.put("caseStep", "caseStepScript");
        caseTemplate.put("caseData", "caseData");
        caseTemplate.put("caseExpect", "caseExpect");
        caseTemplate.put("imgUrls", "imgObjects");
        CASE_TEMPLATE_MAP = Collections.unmodifiableMap(caseTemplate);

        CASE_TEMPLATE_REQUIRED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "caseName", "moduleName", "caseExpect")));

        CASE_TEMPLATE_EXCLUDED = Collections.unmodifiableSet(Collections.singleton("defectProcessingCount"));
    }

    private ExcelColumnExportSupport()
    {
    }

    public static List<String> resolveOrderedFieldNames(Map<String, Object> params, Map<String, String> propToField,
            Set<String> requiredFields, Set<String> templateExcludedProps)
    {
        if (params == null || propToField == null)
        {
            return Collections.emptyList();
        }
        JSONArray columns = parseExportColumns(params.get(PARAM_EXPORT_COLUMNS));
        if (columns == null || columns.isEmpty())
        {
            return Collections.emptyList();
        }
        String scope = String.valueOf(params.getOrDefault(PARAM_EXPORT_SCOPE, SCOPE_DATA));
        if (requiredFields == null)
        {
            requiredFields = Collections.emptySet();
        }
        if (templateExcludedProps == null)
        {
            templateExcludedProps = Collections.emptySet();
        }
        List<String> orderedFieldNames = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++)
        {
            JSONObject col = columns.getJSONObject(i);
            if (col == null)
            {
                continue;
            }
            String prop = col.getString("prop");
            String key = col.getString("key");
            boolean visible = col.getBooleanValue("visible");
            if (SCOPE_IMPORT_TEMPLATE.equals(scope)
                    && (templateExcludedProps.contains(prop) || templateExcludedProps.contains(key)))
            {
                continue;
            }
            String fieldName = resolveFieldName(propToField, prop, key);
            if (fieldName == null)
            {
                continue;
            }
            boolean include;
            if (SCOPE_IMPORT_TEMPLATE.equals(scope))
            {
                include = visible || requiredFields.contains(fieldName);
            }
            else
            {
                include = visible;
            }
            if (include)
            {
                orderedFieldNames.add(fieldName);
            }
        }
        return orderedFieldNames;
    }

    static String resolveFieldName(Map<String, String> propToField, String prop, String key)
    {
        if (StringUtils.isNotEmpty(prop) && propToField.containsKey(prop))
        {
            return propToField.get(prop);
        }
        if (StringUtils.isNotEmpty(key) && propToField.containsKey(key))
        {
            return propToField.get(key);
        }
        return null;
    }

    static JSONArray parseExportColumns(Object raw)
    {
        if (raw == null)
        {
            return null;
        }
        if (raw instanceof JSONArray)
        {
            return (JSONArray) raw;
        }
        if (raw instanceof String)
        {
            String s = ((String) raw).trim();
            if (StringUtils.isEmpty(s))
            {
                return null;
            }
            return JSON.parseArray(s);
        }
        if (raw instanceof Collection)
        {
            return new JSONArray((Collection<?>) raw);
        }
        return JSON.parseArray(JSON.toJSONString(raw));
    }
}
