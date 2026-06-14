package com.cat2bug.web.service.excel;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysCaseStep;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import com.cat2bug.common.utils.poi.ExcelI18n;
import com.cat2bug.common.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;/**
 * 用例 Excel 列定义与读写辅助。
 */
final class CaseExcelColumnSupport {static final String SHEET_NAME = "测试用例";
    static final String TEMPLATE_SHEET_NAME = "测试用例";
    static final String LIST_SHEET_NAME = "_lists";
    static final int DATA_ROWS = 1000;private static final List<String> LEVEL_LABELS = List.of("P0", "P1", "P2", "P3", "P4");private static final List<ColumnDef> EXPORT_COLUMNS = List.of(
            col("caseNum", "case.number", "用例编号", false, 10),
            col("caseName", "case.name_excel", "用例名称(必填)", true, 20),
            col("moduleId", "module", "交付物(必填)", true, 30),
            col("caseLevelName", "case.level", "用例级别", false, 40),
            col("casePreconditions", "case.prerequisite", "前置条件", false, 50),
            col("caseStep", "case.step", "步骤", false, 60),
            col("caseData", "case.data", "数据", false, 70),
            col("caseExpect", "case.expected_excel", "预期(必填)", true, 80),
            col("imgUrls", "image", "图片", false, 90),
            col("annexUrls", "annex", "附件", false, 100),
            col("caseExportUpdateTime", "update-time", "更新时间", false, 110),
            col("defectProcessingCount", "defect.state", "关联缺陷", false, 120)
    );private static final List<ColumnDef> TEMPLATE_COLUMNS = List.of(
            col("caseName", "case.name_excel", "用例名称(必填)", true, 20),
            col("moduleName", "module", "交付物", true, 30),
            col("caseLevel", "case.level", "用例级别", false, 40),
            col("casePreconditions", "case.prerequisite", "前置条件", false, 50),
            col("caseStepScript", "case.step", "步骤", false, 60),
            col("caseData", "case.data", "数据", false, 70),
            col("caseExpect", "case.expected_excel", "预期(必填)", true, 80),
            col("imgObjects", "image", "图片", false, 90)
    );private CaseExcelColumnSupport() {
    }

    static List<ColumnDef> resolveExportColumns(Map<String, Object> params, Locale locale) {
        List<String> fields = ExcelColumnExportSupport.resolveOrderedFieldNames(
                params, ExcelColumnExportSupport.CASE_DATA_MAP, null, null);
        if (fields.isEmpty()) {
            return localizeColumns(EXPORT_COLUMNS, locale);
        }
        return localizeColumns(filterByFieldNames(EXPORT_COLUMNS, fields), locale);
    }

    static List<ColumnDef> resolveTemplateColumns(Map<String, Object> params, Locale locale) {
        List<String> fields = ExcelColumnExportSupport.resolveOrderedFieldNames(
                params,
                ExcelColumnExportSupport.CASE_TEMPLATE_MAP,
                ExcelColumnExportSupport.CASE_TEMPLATE_REQUIRED,
                ExcelColumnExportSupport.CASE_TEMPLATE_EXCLUDED);
        if (fields.isEmpty()) {
            return localizeColumns(TEMPLATE_COLUMNS, locale);
        }
        return localizeColumns(filterByFieldNames(TEMPLATE_COLUMNS, fields), locale);
    }

    private static List<ColumnDef> localizeColumns(List<ColumnDef> columns, Locale locale) {
        List<ColumnDef> localized = new ArrayList<>(columns.size());
        for (ColumnDef column : columns) {
            localized.add(new ColumnDef(
                    column.fieldName(),
                    column.i18nKey(),
                    ExcelI18n.header(column.i18nKey(), column.defaultHeader(), locale),
                    column.required(),
                    column.sort()));
        }
        return localized;
    }

    private static List<ColumnDef> filterByFieldNames(List<ColumnDef> defaults, List<String> fieldNames) {
        Map<String, ColumnDef> byField = defaults.stream()
                .collect(Collectors.toMap(ColumnDef::fieldName, c -> c, (a, b) -> a, LinkedHashMap::new));
        List<ColumnDef> resolved = new ArrayList<>();
        for (String field : fieldNames) {
            ColumnDef def = byField.get(field);
            if (def != null) {
                resolved.add(def);
            }
        }
        return resolved.isEmpty() ? defaults : resolved;
    }

    static String formatExportValue(SysCase item, ColumnDef column, Map<Long, String> modulePaths) {
        return switch (column.fieldName()) {
            case "caseNum" -> item.getCaseNum() == null ? "" : String.valueOf(item.getCaseNum());
            case "caseName" -> nullToEmpty(item.getCaseName());
            case "moduleId" -> {
                if (item.getModuleId() == null) {
                    yield "";
                }
                yield nullToEmpty(modulePaths.get(item.getModuleId()));
            }
            case "caseLevelName" -> nullToEmpty(item.getCaseLevelName());
            case "casePreconditions" -> nullToEmpty(item.getCasePreconditions());
            case "caseStep" -> formatCaseSteps(item.getCaseStep());
            case "caseData" -> nullToEmpty(item.getCaseData());
            case "caseExpect" -> nullToEmpty(item.getCaseExpect());
            case "imgUrls" -> nullToEmpty(item.getImgUrls());
            case "annexUrls" -> nullToEmpty(item.getAnnexUrls());
            case "caseExportUpdateTime" -> formatDate(item.getCaseExportUpdateTime());
            case "defectProcessingCount" -> String.valueOf(item.getDefectProcessingCount());
            default -> "";
        };
    }

    static void applyImportValue(SysCase item, ColumnDef column, String raw,
                                 Map<String, Long> modulePathToId) {
        if (raw == null) {
            return;
        }
        String value = raw.trim();
        switch (column.fieldName()) {
            case "caseName" -> item.setCaseName(value);
            case "moduleName" -> {
                item.setModuleName(value);
                if (modulePathToId.containsKey(value)) {
                    item.setModuleId(modulePathToId.get(value));
                }
            }
            case "caseLevel" -> item.setCaseLevel(parseCaseLevel(value));
            case "casePreconditions" -> item.setCasePreconditions(value);
            case "caseStepScript" -> item.setCaseStepScript(value);
            case "caseData" -> item.setCaseData(value);
            case "caseExpect" -> item.setCaseExpect(value);
            case "imgObjects" -> item.setImgObjects(value);
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

    static int moduleColumnIndex(List<ColumnDef> columns) {
        for (int i = 0; i < columns.size(); i++) {
            if ("moduleName".equals(columns.get(i).fieldName())) {
                return i;
            }
        }
        return -1;
    }

    static int levelColumnIndex(List<ColumnDef> columns) {
        for (int i = 0; i < columns.size(); i++) {
            if ("caseLevel".equals(columns.get(i).fieldName())) {
                return i;
            }
        }
        return -1;
    }

    static List<String> levelLabels() {
        return LEVEL_LABELS;
    }

    private static String formatCaseSteps(List<SysCaseStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            SysCaseStep step = steps.get(i);
            if (step == null) {
                continue;
            }
            if (StringUtils.isNotBlank(step.getStepDescribe()) || StringUtils.isNotBlank(step.getStepExpect())) {
                String enterKey = (i == steps.size() - 1) ? "" : "\n";
                sb.append(String.format("%s%s%s%s",
                        StringUtils.isBlank(step.getStepDescribe()) ? "" : step.getStepDescribe(),
                        StringUtils.isBlank(step.getStepExpect()) ? "" : "---",
                        StringUtils.isBlank(step.getStepExpect()) ? "" : step.getStepExpect(),
                        enterKey));
            }
        }
        return sb.toString();
    }

    private static Long parseCaseLevel(String value) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group()) + 1;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static String normalizeHeader(String header) {
        return header.replace("(必填)", "").trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static ColumnDef col(String fieldName, String i18nKey, String defaultHeader, boolean required, int sort) {
        return new ColumnDef(fieldName, i18nKey, defaultHeader, required, sort);
    }record ColumnDef(String fieldName, String i18nKey, String header, boolean required, int sort) {
        String defaultHeader() {
            return header;
        }
    }
}
