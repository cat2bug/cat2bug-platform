package com.cat2bug.web.service.excel;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysDictData;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import com.cat2bug.common.utils.poi.ExcelI18n;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.util.DefectCustomFieldFastExcelSupport;
import com.cat2bug.common.core.domain.excel.DefectImportLabelResolver;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;/**
 * 缺陷 Excel 列定义、i18n 表头与读写辅助。
 */
final class DefectExcelColumnSupport {static final String SHEET_NAME = "缺陷数据";
    static final int DATA_ROWS = 1000;private static final List<ColumnDef> DEFAULT_EXPORT_COLUMNS = List.of(
            col("projectNum", "id", "编号", false),
            col("defectType", "type", "类型", false),
            col("defectName", "defect.name", "缺陷名称", false),
            col("defectDescribe", "describe", "描述", false),
            col("defectLevel", "defect.level", "缺陷等级", false),
            col("defectState", "defect.state", "缺陷状态", false),
            col("moduleName", "module", "交付物", false),
            col("handleByNames", "handle-by", "处理人", true)
    );private static final List<ColumnDef> DEFAULT_TEMPLATE_COLUMNS = List.of(
            col("defectTypeImportName", "type", "类型", false),
            col("defectName", "defect.name", "缺陷名称", true),
            col("defectDescribe", "describe", "描述", false),
            col("defectLevel", "defect.level", "缺陷等级", false),
            col("defectStateImportName", "defect.state", "缺陷状态", false),
            col("moduleName", "module", "交付物", false),
            col("handleByNames", "handle-by", "处理人", true)
    );private static final Map<String, ColumnDef> EXPORT_BY_FIELD = indexByField(DEFAULT_EXPORT_COLUMNS);
    private static final Map<String, ColumnDef> TEMPLATE_BY_FIELD = indexByField(DEFAULT_TEMPLATE_COLUMNS);private DefectExcelColumnSupport() {
    }

    static List<ColumnDef> resolveExportColumns(Map<String, Object> params, Locale locale,
                                                List<SysProjectDefectField> customFieldDefs) {
        return resolveColumns(params, ExcelColumnExportSupport.DEFECT_DATA_MAP, null, null,
                DEFAULT_EXPORT_COLUMNS, EXPORT_BY_FIELD, customFieldDefs, locale);
    }

    static List<ColumnDef> resolveTemplateColumns(Map<String, Object> params, Locale locale,
                                                  List<SysProjectDefectField> customFieldDefs) {
        if (params == null) {
            params = Map.of();
        }
        params = new HashMap<>(params);
        params.putIfAbsent(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE);
        return resolveColumns(params, ExcelColumnExportSupport.DEFECT_TEMPLATE_MAP,
                ExcelColumnExportSupport.DEFECT_TEMPLATE_REQUIRED, null,
                DEFAULT_TEMPLATE_COLUMNS, TEMPLATE_BY_FIELD, customFieldDefs, locale);
    }/** 导入解析：合并导出/模版列定义，支持带编号列的更新导入与导出文件回导 */
    static List<ColumnDef> resolveImportColumns(Map<String, Object> params, Locale locale,
                                                List<SysProjectDefectField> customFieldDefs) {
        List<ColumnDef> exportCols = resolveExportColumns(params, locale, customFieldDefs);
        List<ColumnDef> templateCols = resolveTemplateColumns(params, locale, customFieldDefs);
        Map<String, ColumnDef> merged = new LinkedHashMap<>();
        for (ColumnDef column : exportCols) {
            merged.put(columnKey(column), column);
        }
        for (ColumnDef column : templateCols) {
            merged.putIfAbsent(columnKey(column), column);
        }
        return new ArrayList<>(merged.values());
    }

    private static String columnKey(ColumnDef column) {
        if (column.custom()) {
            return "custom:" + column.customDef().getFieldKey();
        }
        return column.fieldName();
    }

    private static List<ColumnDef> resolveColumns(Map<String, Object> params, Map<String, String> propToField,
                                                  java.util.Set<String> requiredFields,
                                                  java.util.Set<String> templateExcludedProps,
                                                  List<ColumnDef> defaults, Map<String, ColumnDef> byField,
                                                  List<SysProjectDefectField> customFieldDefs, Locale locale) {
        List<DefectCustomFieldFastExcelSupport.ColumnSlot> slots = DefectCustomFieldFastExcelSupport.buildOrderedSlots(
                params, propToField, requiredFields, templateExcludedProps);
        Map<String, SysProjectDefectField> defByKey = indexDefinitions(customFieldDefs);if (slots.isEmpty()) {
            List<ColumnDef> columns = localize(defaults, locale);
            appendAllCustomColumns(columns, customFieldDefs, locale);
            return columns;
        }List<ColumnDef> resolved = new ArrayList<>();
        for (DefectCustomFieldFastExcelSupport.ColumnSlot slot : slots) {
            if (slot.isCustom()) {
                SysProjectDefectField def = defByKey.get(slot.getFieldKey());
                if (def != null) {
                    resolved.add(customColumn(def, locale));
                }
                continue;
            }
            ColumnDef column = byField.get(slot.getStandardFieldName());
            if (column != null) {
                resolved.add(localize(column, locale));
            }
        }
        return resolved.isEmpty() ? localize(defaults, locale) : resolved;
    }

    private static void appendAllCustomColumns(List<ColumnDef> columns, List<SysProjectDefectField> fieldDefinitions,
                                               Locale locale) {
        if (fieldDefinitions == null) {
            return;
        }
        for (SysProjectDefectField def : fieldDefinitions) {
            if (def == null || StringUtils.isEmpty(def.getFieldKey())) {
                continue;
            }
            columns.add(customColumn(def, locale));
        }
    }

    static String formatExportValue(SysDefect defect, ColumnDef column, List<SysDictData> levelDict) {
        if (column.custom()) {
            Map<String, Object> cf = defect.getCustomFields();
            Object val = cf != null ? cf.get(column.customDef().getFieldKey()) : null;
            return DefectCustomFieldFastExcelSupport.formatExportValue(val, column.customDef());
        }
        return switch (column.fieldName()) {
            case "projectNum" -> defect.getProjectNum() == null ? "" : String.valueOf(defect.getProjectNum());
            case "defectType" -> DefectImportLabelResolver.formatType(defect.getDefectType());
            case "defectTypeImportName" -> DefectImportLabelResolver.formatType(defect.getDefectType());
            case "defectName" -> nullToEmpty(defect.getDefectName());
            case "defectDescribe" -> nullToEmpty(defect.getDefectDescribe());
            case "defectLevel" -> DefectImportLabelResolver.formatLevel(defect.getDefectLevel(), levelDict);
            case "defectState" -> DefectImportLabelResolver.formatState(defect.getDefectState());
            case "defectStateImportName" -> DefectImportLabelResolver.formatState(defect.getDefectState());
            case "moduleName" -> {
                String moduleLabel = StringUtils.isNotEmpty(defect.getModulePath())
                        ? defect.getModulePath() : defect.getModuleName();
                yield nullToEmpty(moduleLabel);
            }
            case "moduleVersion" -> nullToEmpty(defect.getModuleVersion());
            case "imgUrls" -> nullToEmpty(defect.getImgUrls());
            case "annexUrls" -> nullToEmpty(defect.getAnnexUrls());
            case "updateTime" -> formatDate(defect.getUpdateTime());
            case "planStartTime" -> formatDate(defect.getPlanStartTime());
            case "planEndTime" -> formatDate(defect.getPlanEndTime());
            case "createMemberName" -> nullToEmpty(defect.getCreateMemberName());
            case "handleByNames" -> nullToEmpty(defect.getHandleByNames());
            default -> "";
        };
    }

    static void applyImportValue(SysDefect defect, ColumnDef column, String raw) {
        if (raw == null || column.custom()) {
            return;
        }
        String value = raw.trim();
        switch (column.fieldName()) {
            case "projectNum" -> {
                try {
                    defect.setProjectNum(Long.valueOf(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "defectTypeImportName" -> defect.setDefectTypeImportName(value);
            case "defectType" -> defect.setDefectTypeImportName(value);
            case "defectName" -> defect.setDefectName(value);
            case "defectDescribe" -> defect.setDefectDescribe(value);
            case "defectLevel" -> defect.setDefectLevel(value);
            case "defectStateImportName" -> defect.setDefectStateImportName(value);
            case "defectState" -> defect.setDefectStateImportName(value);
            case "moduleName" -> {
                defect.setModuleName(value);
                defect.setModulePath(value);
            }
            case "moduleVersion" -> defect.setModuleVersion(value);
            case "imgObjects" -> defect.setImgUrls(value);
            case "handleByNames" -> defect.setHandleByNames(value);
            default -> {
            }
        }
    }

    static ColumnDef findByHeader(String header, List<ColumnDef> columns) {
        if (StringUtils.isBlank(header)) {
            return null;
        }
        String normalized = normalizeHeader(header);
        for (ColumnDef column : columns) {
            if (normalizeHeader(column.header()).equals(normalized)) {
                return column;
            }
            if (StringUtils.isNotEmpty(column.i18nKey())) {
                for (String translation : ExcelI18n.allTranslations(column.i18nKey())) {
                    if (normalizeHeader(translation).equals(normalized)) {
                        return column;
                    }
                }
            }
        }
        return null;
    }

    static int columnIndex(List<ColumnDef> columns, String fieldName) {
        for (int i = 0; i < columns.size(); i++) {
            if (fieldName.equals(columns.get(i).fieldName())) {
                return i;
            }
        }
        return -1;
    }

    private static ColumnDef customColumn(SysProjectDefectField def, Locale locale) {
        boolean required = def.getRequired() != null && def.getRequired() == 1;
        return new ColumnDef("custom:" + def.getFieldKey(), null, def.getFieldLabel(), required, true, def);
    }

    private static List<ColumnDef> localize(List<ColumnDef> columns, Locale locale) {
        List<ColumnDef> localized = new ArrayList<>(columns.size());
        for (ColumnDef column : columns) {
            localized.add(localize(column, locale));
        }
        return localized;
    }

    private static ColumnDef localize(ColumnDef column, Locale locale) {
        if (column.custom()) {
            return column;
        }
        String header = column.required()
                ? ExcelI18n.header(column.i18nKey(), column.defaultHeader(), locale)
                : ExcelI18n.header(column.i18nKey(), column.defaultHeader(), locale);
        return new ColumnDef(column.fieldName(), column.i18nKey(), header, column.required(), false, null);
    }

    private static Map<String, SysProjectDefectField> indexDefinitions(List<SysProjectDefectField> definitions) {
        Map<String, SysProjectDefectField> map = new HashMap<>();
        if (definitions == null) {
            return map;
        }
        for (SysProjectDefectField def : definitions) {
            if (def != null && StringUtils.isNotEmpty(def.getFieldKey())) {
                map.put(def.getFieldKey(), def);
            }
        }
        return map;
    }

    private static Map<String, ColumnDef> indexByField(List<ColumnDef> columns) {
        return columns.stream()
                .collect(Collectors.toMap(ColumnDef::fieldName, c -> c, (a, b) -> a, LinkedHashMap::new));
    }

    private static String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private static String normalizeHeader(String header) {
        return header.replace("(必填)", "").replace("(required)", "").trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static ColumnDef col(String fieldName, String i18nKey, String defaultHeader, boolean required) {
        return new ColumnDef(fieldName, i18nKey, defaultHeader, required, false, null);
    }record ColumnDef(String fieldName, String i18nKey, String header, boolean required, boolean custom,
                     SysProjectDefectField customDef) {
        String defaultHeader() {
            return header;
        }
    }
}
