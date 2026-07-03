package com.cat2bug.system.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.system.domain.SysProjectDefectField;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * 缺陷 Excel 导出/导入模版中的自定义字段列（与前端 custom:{fieldKey} 列配置一致）。
 */
public final class DefectCustomFieldExcelSupport {

    public static final String CUSTOM_COLUMN_KEY_PREFIX = "custom:";
    public static final String CUSTOM_COLUMN_PROP_PREFIX = "custom_";

    private DefectCustomFieldExcelSupport() {
    }

    /**
     * 在 {@link ExcelUtil#writeSheet()} 之后插入自定义列（保持与 exportColumns 顺序一致）。
     */
    public static void applyCustomColumnsAfterWrite(ExcelUtil<?> util, Map<String, Object> params,
            Map<String, String> propToField, Set<String> requiredFields, Set<String> templateExcludedProps,
            List<?> dataRows, List<SysProjectDefectField> fieldDefinitions) {
        if (util == null || params == null || propToField == null) {
            return;
        }
        com.alibaba.fastjson2.JSONArray exportColumns = parseExportColumns(
                params.get(ExcelColumnExportSupport.PARAM_EXPORT_COLUMNS));
        List<ColumnSlot> slots = buildOrderedSlotsInternal(params, propToField, requiredFields, templateExcludedProps);
        List<ColumnSlot> customSlots = slots.stream().filter(ColumnSlot::isCustom).collect(Collectors.toList());
        Map<String, SysProjectDefectField> defByKey = indexDefinitions(fieldDefinitions);
        Sheet sheet = util.getSheet();
        if (sheet == null) {
            return;
        }
        int headerRowIndex = util.getHeaderRowIndex();
        int dataStartRow = util.getDataStartRowIndex();
        if (customSlots.isEmpty()) {
            if (exportColumns == null || exportColumns.isEmpty()) {
                appendAllCustomColumnsAtEnd(util, sheet, headerRowIndex, dataStartRow, fieldDefinitions, dataRows);
            }
            return;
        }
        int insertCol = 0;
        for (ColumnSlot slot : slots) {
            if (!slot.isCustom()) {
                insertCol++;
                continue;
            }
            SysProjectDefectField def = defByKey.get(slot.fieldKey);
            if (def == null) {
                continue;
            }
            insertCustomColumn(util, sheet, headerRowIndex, dataStartRow, insertCol, def, dataRows);
            insertCol++;
        }
    }

    /** exportColumns 未包含自定义列时，在标准列之后追加全部已启用自定义字段 */
    private static void appendAllCustomColumnsAtEnd(ExcelUtil<?> util, Sheet sheet, int headerRowIndex, int dataStartRow,
            List<SysProjectDefectField> fieldDefinitions, List<?> dataRows) {
        if (fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return;
        }
        Row header = sheet.getRow(headerRowIndex);
        int insertCol = header == null ? 0 : Math.max(0, header.getLastCellNum());
        for (SysProjectDefectField def : fieldDefinitions) {
            if (def == null || StringUtils.isEmpty(def.getFieldKey())) {
                continue;
            }
            insertCustomColumn(util, sheet, headerRowIndex, dataStartRow, insertCol, def, dataRows);
            insertCol++;
        }
    }

    /**
     * 从导入文件中读取自定义列并写入每条缺陷的 customFields。
     */
    public static void mergeCustomFieldsFromImport(InputStream in, List<SysDefect> defects,
            List<SysProjectDefectField> fieldDefinitions) throws Exception {
        if (in == null || defects == null || defects.isEmpty() || fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return;
        }
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                return;
            }
            int headerRowIndex = findHeaderRowIndex(sheet);
            Row header = sheet.getRow(headerRowIndex);
            if (header == null) {
                return;
            }
            Map<Integer, SysProjectDefectField> colIndexToDef = mapCustomColumnsByHeader(header, fieldDefinitions);
            if (colIndexToDef.isEmpty()) {
                return;
            }
            DataFormatter formatter = new DataFormatter();
            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }
                int defectIndex = r - headerRowIndex - 1;
                if (defectIndex < 0 || defectIndex >= defects.size()) {
                    break;
                }
                SysDefect defect = defects.get(defectIndex);
                Map<String, Object> custom = defect.getCustomFields() != null
                        ? new LinkedHashMap<>(defect.getCustomFields()) : new LinkedHashMap<>();
                for (Map.Entry<Integer, SysProjectDefectField> e : colIndexToDef.entrySet()) {
                    Cell cell = row.getCell(e.getKey());
                    String text = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    Object parsed = parseImportCellValue(text, e.getValue());
                    if (parsed != null && !isEmptyValue(parsed)) {
                        custom.put(e.getValue().getFieldKey(), parsed);
                    }
                }
                if (!custom.isEmpty()) {
                    defect.setCustomFields(custom);
                }
            }
        }
    }

    /**
     * 从二维表文本合并自定义字段（B9 FastExcel：无 POI 依赖）。
     */
    public static void mergeCustomFieldsFromRows(List<List<String>> rows, List<SysDefect> defects,
            List<SysProjectDefectField> fieldDefinitions) {
        if (rows == null || rows.isEmpty() || defects == null || defects.isEmpty()
                || fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return;
        }
        int headerRowIndex = findHeaderRowIndex(rows);
        if (headerRowIndex < 0 || headerRowIndex >= rows.size()) {
            return;
        }
        List<String> headerCells = rows.get(headerRowIndex);
        Map<Integer, SysProjectDefectField> colIndexToDef = mapCustomColumnsByHeader(headerCells, fieldDefinitions);
        if (colIndexToDef.isEmpty()) {
            return;
        }
        for (int r = headerRowIndex + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row == null || isEmptyTextRow(row)) {
                continue;
            }
            int defectIndex = r - headerRowIndex - 1;
            if (defectIndex < 0 || defectIndex >= defects.size()) {
                break;
            }
            SysDefect defect = defects.get(defectIndex);
            Map<String, Object> custom = defect.getCustomFields() != null
                    ? new LinkedHashMap<>(defect.getCustomFields()) : new LinkedHashMap<>();
            for (Map.Entry<Integer, SysProjectDefectField> e : colIndexToDef.entrySet()) {
                String text = cellText(row, e.getKey());
                Object parsed = parseImportCellValue(text, e.getValue());
                if (parsed != null && !isEmptyValue(parsed)) {
                    custom.put(e.getValue().getFieldKey(), parsed);
                }
            }
            if (!custom.isEmpty()) {
                defect.setCustomFields(custom);
            }
        }
    }

    /**
     * B9：供 {@link com.cat2bug.web.service.excel.DefectExcelColumnSupport} 读取列顺序。
     */
    public static List<ColumnSlot> buildOrderedSlots(Map<String, Object> params, Map<String, String> propToField,
            Set<String> requiredFields, Set<String> templateExcludedProps) {
        return buildOrderedSlotsInternal(params, propToField, requiredFields, templateExcludedProps);
    }

    private static List<ColumnSlot> buildOrderedSlotsInternal(Map<String, Object> params, Map<String, String> propToField,
            Set<String> requiredFields, Set<String> templateExcludedProps) {
        com.alibaba.fastjson2.JSONArray columns = parseExportColumns(params.get(ExcelColumnExportSupport.PARAM_EXPORT_COLUMNS));
        if (columns == null || columns.isEmpty()) {
            return Collections.emptyList();
        }
        String scope = String.valueOf(params.getOrDefault(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE,
                ExcelColumnExportSupport.SCOPE_DATA));
        if (requiredFields == null) {
            requiredFields = Collections.emptySet();
        }
        if (templateExcludedProps == null) {
            templateExcludedProps = Collections.emptySet();
        }
        List<ColumnSlot> slots = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            com.alibaba.fastjson2.JSONObject col = columns.getJSONObject(i);
            if (col == null) {
                continue;
            }
            String prop = col.getString("prop");
            String key = col.getString("key");
            boolean visible = col.getBooleanValue("visible");
            if (ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE.equals(scope)
                    && (templateExcludedProps.contains(prop) || templateExcludedProps.contains(key))) {
                continue;
            }
            String fieldKey = resolveCustomFieldKey(prop, key);
            if (fieldKey != null) {
                boolean include = ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE.equals(scope)
                        ? visible || isCustomRequired(col)
                        : visible;
                if (include) {
                    slots.add(ColumnSlot.custom(fieldKey));
                }
                continue;
            }
            String fieldName = resolveStandardFieldName(propToField, prop, key);
            if (fieldName == null) {
                continue;
            }
            boolean include;
            if (ExcelColumnExportSupport.SCOPE_IMPORT_TEMPLATE.equals(scope)) {
                include = visible || requiredFields.contains(fieldName);
            } else {
                include = visible;
            }
            if (include) {
                slots.add(ColumnSlot.standard(fieldName));
            }
        }
        return slots;
    }

    private static boolean isCustomRequired(com.alibaba.fastjson2.JSONObject col) {
        return col != null && col.getBooleanValue("required");
    }

    private static String resolveStandardFieldName(Map<String, String> propToField, String prop, String key) {
        if (StringUtils.isNotEmpty(prop) && propToField.containsKey(prop)) {
            return propToField.get(prop);
        }
        if (StringUtils.isNotEmpty(key) && propToField.containsKey(key)) {
            return propToField.get(key);
        }
        return null;
    }

    public static String resolveCustomFieldKey(String prop, String key) {
        if (StringUtils.isNotEmpty(key) && key.startsWith(CUSTOM_COLUMN_KEY_PREFIX)) {
            return key.substring(CUSTOM_COLUMN_KEY_PREFIX.length());
        }
        if (StringUtils.isNotEmpty(prop) && prop.startsWith(CUSTOM_COLUMN_PROP_PREFIX)) {
            return prop.substring(CUSTOM_COLUMN_PROP_PREFIX.length());
        }
        if (StringUtils.isNotEmpty(prop) && prop.startsWith(CUSTOM_COLUMN_KEY_PREFIX)) {
            return prop.substring(CUSTOM_COLUMN_KEY_PREFIX.length());
        }
        return null;
    }

    private static com.alibaba.fastjson2.JSONArray parseExportColumns(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof com.alibaba.fastjson2.JSONArray) {
            return (com.alibaba.fastjson2.JSONArray) raw;
        }
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            if (StringUtils.isEmpty(s)) {
                return null;
            }
            return com.alibaba.fastjson2.JSON.parseArray(s);
        }
        if (raw instanceof java.util.Collection) {
            return new com.alibaba.fastjson2.JSONArray((java.util.Collection<?>) raw);
        }
        return com.alibaba.fastjson2.JSON.parseArray(com.alibaba.fastjson2.JSON.toJSONString(raw));
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

    private static void insertCustomColumn(ExcelUtil<?> util, Sheet sheet, int headerRowIndex, int dataStartRow,
            int insertAt, SysProjectDefectField def, List<?> dataRows) {
        int lastCol = Math.max(0, getLastColumnIndex(sheet, headerRowIndex));
        if (insertAt <= lastCol) {
            sheet.shiftColumns(insertAt, lastCol, 1);
        }
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            headerRow = sheet.createRow(headerRowIndex);
        }
        Cell headCell = headerRow.createCell(insertAt);
        headCell.setCellValue(def.getFieldLabel());
        boolean required = def.getRequired() != null && def.getRequired() == 1;
        headCell.setCellStyle(buildCustomHeaderStyle(sheet.getWorkbook(), required));
        CellStyle dataStyle = copyNeighborCellStyle(
                dataRows.isEmpty() ? headerRow : sheet.getRow(dataStartRow), insertAt);
        if (dataStyle == null) {
            dataStyle = buildDefaultDataStyle(sheet.getWorkbook());
        }
        for (int i = 0; i < dataRows.size(); i++) {
            int rowIndex = dataStartRow + i;
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }
            Object item = dataRows.get(i);
            String text = "";
            if (item instanceof SysDefect) {
                Map<String, Object> cf = ((SysDefect) item).getCustomFields();
                Object val = cf != null ? cf.get(def.getFieldKey()) : null;
                text = formatExportValue(val, def);
            }
            Cell cell = row.createCell(insertAt);
            cell.setCellStyle(dataStyle);
            cell.setCellValue(text == null ? "" : text);
        }
        applyEnumColumnValidation(util, sheet, insertAt, dataStartRow, def);
    }

    /** 枚举自定义字段：数据区下拉（选项为 label，与导出显示及导入解析一致） */
    private static void applyEnumColumnValidation(ExcelUtil<?> util, Sheet sheet, int columnIndex,
            int dataStartRow, SysProjectDefectField def) {
        if (util == null || sheet == null || def == null || !"enum".equals(def.getFieldType())) {
            return;
        }
        String[] combo = resolveEnumComboLabels(def);
        if (combo.length == 0) {
            return;
        }
        int endRow = ExcelUtil.sheetSize;
        util.applyColumnListValidation(columnIndex, combo, dataStartRow, endRow);
    }

    /** 下拉显示枚举 label（无 label 时用 key） */
    public static String[] resolveEnumComboLabels(SysProjectDefectField def) {
        if (def == null || !"enum".equals(def.getFieldType())) {
            return new String[0];
        }
        JSONObject cfg = def.getTypeConfig() == null ? new JSONObject() : new JSONObject(def.getTypeConfig());
        JSONArray options = cfg.getJSONArray("options");
        if (options == null || options.isEmpty()) {
            return new String[0];
        }
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            JSONObject opt = options.getJSONObject(i);
            if (opt == null) {
                continue;
            }
            Object label = opt.get("label");
            String text = label != null && StringUtils.isNotEmpty(String.valueOf(label))
                    ? String.valueOf(label).trim()
                    : String.valueOf(opt.get("key")).trim();
            if (StringUtils.isNotEmpty(text) && !labels.contains(text)) {
                labels.add(text);
            }
        }
        return labels.toArray(new String[0]);
    }

    /** 灰底表头；仅必填自定义字段标题为红色（不复制相邻必填列的红字样式） */
    private static CellStyle buildCustomHeaderStyle(Workbook wb, boolean required) {
        CellStyle base = buildDefaultHeaderStyle(wb);
        if (!required) {
            return base;
        }
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        base.setFont(font);
        return base;
    }

    private static CellStyle copyNeighborCellStyle(Row row, int insertAt) {
        if (row == null || insertAt <= 0) {
            return null;
        }
        Cell neighbor = row.getCell(insertAt - 1);
        if (neighbor == null || neighbor.getCellStyle() == null) {
            return null;
        }
        Workbook wb = row.getSheet().getWorkbook();
        CellStyle style = wb.createCellStyle();
        style.cloneStyleFrom(neighbor.getCellStyle());
        return style;
    }

    private static CellStyle buildDefaultHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        setThinBorders(style);
        return style;
    }

    private static CellStyle buildDefaultDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        setThinBorders(style);
        return style;
    }

    private static void setThinBorders(CellStyle style) {
        short borderColor = IndexedColors.GREY_50_PERCENT.getIndex();
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(borderColor);
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(borderColor);
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(borderColor);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(borderColor);
    }

    private static int getLastColumnIndex(Sheet sheet, int headerRowIndex) {
        Row header = sheet.getRow(headerRowIndex);
        if (header == null) {
            return 0;
        }
        return Math.max(0, header.getLastCellNum() - 1);
    }

    public static String formatExportValue(Object value, SysProjectDefectField def) {
        if (value == null || isEmptyValue(value)) {
            return "";
        }
        String type = def.getFieldType();
        if ("enum".equals(type)) {
            JSONObject cfg = def.getTypeConfig() == null ? new JSONObject() : new JSONObject(def.getTypeConfig());
            JSONArray options = cfg.getJSONArray("options");
            String sk = String.valueOf(value);
            if (options != null) {
                for (int i = 0; i < options.size(); i++) {
                    JSONObject opt = options.getJSONObject(i);
                    if (opt != null && sk.equals(String.valueOf(opt.get("key")))) {
                        Object label = opt.get("label");
                        return label != null ? String.valueOf(label) : sk;
                    }
                }
            }
            return sk;
        }
        if ("boolean".equals(type)) {
            if (value instanceof Boolean) {
                return ((Boolean) value) ? "true" : "false";
            }
            return String.valueOf(value);
        }
        if ("image".equals(type) || "file".equals(type) || "array".equals(type)) {
            if (value instanceof List) {
                return ((List<?>) value).stream().map(String::valueOf).collect(Collectors.joining(", "));
            }
            return String.valueOf(value);
        }
        if ("object".equals(type)) {
            if (value instanceof Map || value instanceof JSONObject) {
                return com.alibaba.fastjson2.JSON.toJSONString(value);
            }
            return String.valueOf(value);
        }
        return String.valueOf(value);
    }

    public static Object parseImportCellValue(String raw, SysProjectDefectField def) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String text = raw.trim();
        String type = def.getFieldType();
        if ("enum".equals(type)) {
            JSONObject cfg = def.getTypeConfig() == null ? new JSONObject() : new JSONObject(def.getTypeConfig());
            JSONArray options = cfg.getJSONArray("options");
            if (options != null) {
                for (int i = 0; i < options.size(); i++) {
                    JSONObject opt = options.getJSONObject(i);
                    if (opt == null) {
                        continue;
                    }
                    String key = opt.getString("key");
                    String label = opt.getString("label");
                    if (text.equals(key) || (label != null && text.equals(label))) {
                        return key;
                    }
                }
            }
            return text;
        }
        if ("boolean".equals(type)) {
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
            return text;
        }
        if ("number".equals(type)) {
            try {
                if (text.contains(".")) {
                    return Double.parseDouble(text);
                }
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                return text;
            }
        }
        if ("image".equals(type) || "file".equals(type) || "array".equals(type)) {
            String[] parts = text.split("[,，]");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                String s = p.trim();
                if (StringUtils.isNotEmpty(s)) {
                    list.add(s);
                }
            }
            return list.isEmpty() ? null : list;
        }
        return text;
    }

    private static Map<Integer, SysProjectDefectField> mapCustomColumnsByHeader(List<String> headerCells,
            List<SysProjectDefectField> fieldDefinitions) {
        Map<Integer, SysProjectDefectField> map = new HashMap<>();
        Set<String> standardHeaders = buildStandardImportHeaderTexts();
        Map<String, SysProjectDefectField> byKey = new HashMap<>();
        Map<String, SysProjectDefectField> byLabel = new HashMap<>();
        for (SysProjectDefectField def : fieldDefinitions) {
            if (def == null) {
                continue;
            }
            if (StringUtils.isNotEmpty(def.getFieldKey())) {
                byKey.put(def.getFieldKey().trim(), def);
            }
            if (StringUtils.isNotEmpty(def.getFieldLabel())) {
                byLabel.put(def.getFieldLabel().trim(), def);
            }
        }
        for (int c = 0; c < headerCells.size(); c++) {
            String headerText = cellText(headerCells, c);
            if (StringUtils.isEmpty(headerText) || standardHeaders.contains(headerText)) {
                continue;
            }
            SysProjectDefectField def = byKey.get(headerText);
            if (def == null) {
                def = byLabel.get(headerText);
            }
            if (def != null) {
                map.put(c, def);
            }
        }
        return map;
    }

    private static Map<Integer, SysProjectDefectField> mapCustomColumnsByHeader(Row header,
            List<SysProjectDefectField> fieldDefinitions) {
        Map<Integer, SysProjectDefectField> map = new HashMap<>();
        Set<String> standardHeaders = buildStandardImportHeaderTexts();
        DataFormatter formatter = new DataFormatter();
        Map<String, SysProjectDefectField> byKey = new HashMap<>();
        Map<String, SysProjectDefectField> byLabel = new HashMap<>();
        for (SysProjectDefectField def : fieldDefinitions) {
            if (def == null) {
                continue;
            }
            if (StringUtils.isNotEmpty(def.getFieldKey())) {
                byKey.put(def.getFieldKey().trim(), def);
            }
            if (StringUtils.isNotEmpty(def.getFieldLabel())) {
                byLabel.put(def.getFieldLabel().trim(), def);
            }
        }
        short last = header.getLastCellNum();
        for (int c = 0; c < last; c++) {
            Cell cell = header.getCell(c);
            if (cell == null) {
                continue;
            }
            String headerText = formatter.formatCellValue(cell).trim();
            if (StringUtils.isEmpty(headerText) || standardHeaders.contains(headerText)) {
                continue;
            }
            SysProjectDefectField def = byKey.get(headerText);
            if (def == null) {
                def = byLabel.get(headerText);
            }
            if (def != null) {
                map.put(c, def);
            }
        }
        return map;
    }

    private static Set<String> buildStandardImportHeaderTexts() {
        Set<String> set = new HashSet<>();
        set.add(com.cat2bug.common.utils.MessageUtils.message("type"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("defect.name"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("describe"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("defect.level"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("defect.state"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("module"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("version"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("image"));
        set.add(com.cat2bug.common.utils.MessageUtils.message("handle-by"));
        return set;
    }

    private static int findHeaderRowIndex(List<List<String>> rows) {
        int limit = Math.min(5, rows.size());
        for (int r = 0; r < limit; r++) {
            List<String> row = rows.get(r);
            if (row == null) {
                continue;
            }
            for (String cell : row) {
                if (StringUtils.isNotEmpty(cell)) {
                    return r;
                }
            }
        }
        return 0;
    }

    private static boolean isEmptyTextRow(List<String> row) {
        if (row == null) {
            return true;
        }
        for (String cell : row) {
            if (StringUtils.isNotBlank(cell)) {
                return false;
            }
        }
        return true;
    }

    private static String cellText(List<String> row, int columnIndex) {
        if (row == null || columnIndex < 0 || columnIndex >= row.size()) {
            return "";
        }
        String value = row.get(columnIndex);
        return value == null ? "" : value.trim();
    }

    private static int findHeaderRowIndex(Sheet sheet) {
        for (int r = 0; r <= Math.min(5, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (int c = 0; c < row.getLastCellNum(); c++) {
                Cell cell = row.getCell(c);
                if (cell != null && StringUtils.isNotEmpty(new DataFormatter().formatCellValue(cell))) {
                    return r;
                }
            }
        }
        return 0;
    }

    private static boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && StringUtils.isNotEmpty(formatter.formatCellValue(cell).trim())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return StringUtils.isBlank((String) value);
        }
        if (value instanceof List) {
            return ((List<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty();
        }
        return false;
    }

    public static final class ColumnSlot {
        private final boolean custom;
        private final String fieldKey;
        private final String standardFieldName;

        private ColumnSlot(boolean custom, String fieldKey, String standardFieldName) {
            this.custom = custom;
            this.fieldKey = fieldKey;
            this.standardFieldName = standardFieldName;
        }

        public static ColumnSlot custom(String fieldKey) {
            return new ColumnSlot(true, fieldKey, null);
        }

        public static ColumnSlot standard(String fieldName) {
            return new ColumnSlot(false, null, fieldName);
        }

        public boolean isCustom() {
            return custom;
        }

        public String getFieldKey() {
            return fieldKey;
        }

        public String getStandardFieldName() {
            return standardFieldName;
        }
    }
}
