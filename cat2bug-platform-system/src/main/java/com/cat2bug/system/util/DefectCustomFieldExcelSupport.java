package com.cat2bug.system.util;

import com.cat2bug.common.domain.SysDefect;
import com.cat2bug.common.domain.SysProjectDefectField;
import com.cat2bug.common.excel.ExcelColumnExportSupport;
import com.cat2bug.common.excel.ExcelI18n;
import com.cat2bug.common.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 缺陷 Excel 导出/导入模版中的自定义字段列（与前端 custom:{fieldKey} 列配置一致）。
 */
public final class DefectCustomFieldExcelSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String CUSTOM_COLUMN_KEY_PREFIX = "custom:";
    public static final String CUSTOM_COLUMN_PROP_PREFIX = "custom_";

    private static final Pattern SIMPLE_NUMBER_PATTERN = Pattern.compile("^[0#,.%]*0[0#,.%]*$");
    private static final Pattern DATE_ELAPSED_PATTERN = Pattern.compile("^\\[([hH]+|[mM]+|[sS]+)\\]");
    private static final Pattern DATE_DBNUM_PATTERN = Pattern.compile("^\\[DBNum([123])\\]");
    private static final Pattern DATE_LOCALE_PATTERN = Pattern.compile("^\\[\\$-.*?\\]");
    private static final Pattern DATE_COLOR_PATTERN = Pattern.compile("^\\[[a-zA-Z]+\\]");
    private static final Pattern DATE_CHAR_PATTERN = Pattern.compile("[yYmMdDhHsS]");
    private static final Pattern DATE_FORMAT_PATTERN =
            Pattern.compile("^[\\[\\]yYmMdDhHsS\\-T/年月日,. :\"\\\\]+0*[ampAMP/]*$");

    /** POI 内置日期格式（id → 格式串），FastExcel 对部分内置 id 给出的格式串与 POI 不同。 */
    private static final Map<Integer, String> BUILTIN_DATE_FORMATS = Map.ofEntries(
            Map.entry(14, "m/d/yy"),
            Map.entry(15, "d-mmm-yy"),
            Map.entry(16, "d-mmm"),
            Map.entry(17, "mmm-yy"),
            Map.entry(18, "h:mm AM/PM"),
            Map.entry(19, "h:mm:ss AM/PM"),
            Map.entry(20, "h:mm"),
            Map.entry(21, "h:mm:ss"),
            Map.entry(22, "m/d/yy h:mm"),
            Map.entry(45, "mm:ss"),
            Map.entry(46, "[h]:mm:ss"),
            Map.entry(47, "mm:ss.0"));

    private DefectCustomFieldExcelSupport() {
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

    public static List<ColumnSlot> buildOrderedSlots(Map<String, Object> params, Map<String, String> propToField,
            Set<String> requiredFields, Set<String> templateExcludedProps) {
        JsonNode columns = parseExportColumns(
                params != null ? params.get(ExcelColumnExportSupport.PARAM_EXPORT_COLUMNS) : null);
        if (columns == null || !columns.isArray() || columns.isEmpty()) {
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
        for (JsonNode col : columns) {
            if (col == null || !col.isObject()) {
                continue;
            }
            String prop = textOrNull(col.get("prop"));
            String key = textOrNull(col.get("key"));
            boolean visible = col.path("visible").asBoolean(false);
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

    public static void mergeCustomFieldsFromImport(InputStream in, List<SysDefect> defects,
            List<SysProjectDefectField> fieldDefinitions) throws Exception {
        if (in == null || defects == null || defects.isEmpty() || fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return;
        }
        mergeCustomFieldsFromImport(readFirstSheetCells(in), defects, fieldDefinitions);
    }

    /**
     * 与 {@link #mergeCustomFieldsFromImport(InputStream, List, List)} 相同，但复用已读取的首个工作表单元格
     * （见 {@link #readFirstSheetCells(InputStream)}），避免重复解析文件。
     */
    public static void mergeCustomFieldsFromImport(List<List<Cell>> rows, List<SysDefect> defects,
            List<SysProjectDefectField> fieldDefinitions) {
        if (rows == null || defects == null || defects.isEmpty() || fieldDefinitions == null || fieldDefinitions.isEmpty()) {
            return;
        }
        int headerRowIndex = findHeaderRowIndex(rows);
        List<Cell> header = headerRowIndex < rows.size() ? rows.get(headerRowIndex) : null;
        if (header == null) {
            return;
        }
        Map<Integer, SysProjectDefectField> colIndexToDef = mapCustomColumnsByHeader(header, fieldDefinitions);
        if (colIndexToDef.isEmpty()) {
            return;
        }
        for (int r = headerRowIndex + 1; r < rows.size(); r++) {
            List<Cell> row = rows.get(r);
            if (isEmptyRow(row)) {
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
                String text = formatCellText(cellAt(row, e.getKey())).trim();
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
     * 读取首个工作表的单元格（行、列从 0 开始；缺失的行为空列表，缺失的单元格为 null）。
     * 读取时带上单元格格式，供 {@link #isDateFormatted(Cell)} / {@link #formatCellText(Cell)} 使用。
     */
    public static List<List<Cell>> readFirstSheetCells(InputStream in) throws IOException {
        try (ReadableWorkbook workbook = new ReadableWorkbook(in, new ReadingOptions(true, false))) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            List<List<Cell>> rows = new ArrayList<>();
            try (Stream<org.dhatim.fastexcel.reader.Row> stream = sheet.openStream()) {
                stream.forEach(row -> {
                    int rowIndex = row.getRowNum() - 1;
                    while (rows.size() < rowIndex) {
                        rows.add(new ArrayList<>());
                    }
                    List<Cell> cells = new ArrayList<>();
                    row.forEach(cell -> {
                        if (cell == null) {
                            return;
                        }
                        int columnIndex = cell.getColumnIndex();
                        while (cells.size() <= columnIndex) {
                            cells.add(null);
                        }
                        cells.set(columnIndex, cell);
                    });
                    rows.add(cells);
                });
            }
            return rows;
        }
    }

    public static Cell cellAt(List<Cell> row, int columnIndex) {
        if (row == null || columnIndex < 0 || columnIndex >= row.size()) {
            return null;
        }
        return row.get(columnIndex);
    }

    /**
     * 单元格显示文本（对齐 POI {@code DataFormatter#formatCellValue}，无公式求值器）：
     * 数字按 Excel「常规」格式或简单数字格式输出，日期按单元格日期格式输出，布尔为 TRUE/FALSE，公式返回公式文本。
     */
    public static String formatCellText(Cell cell) {
        if (cell == null || cell.getType() == null) {
            return "";
        }
        switch (cell.getType()) {
            case STRING:
            case ERROR:
                return cell.getText() == null ? "" : cell.getText();
            case NUMBER:
                return formatNumericCell(cell);
            case BOOLEAN:
                return Boolean.TRUE.equals(cell.asBoolean()) ? "TRUE" : "FALSE";
            case FORMULA:
                return cell.getFormula() == null ? "" : cell.getFormula();
            default:
                return "";
        }
    }

    /** 对齐 POI {@code DateUtil#isCellDateFormatted}：内置日期格式或日期类格式串，且数值为有效 Excel 日期。 */
    public static boolean isDateFormatted(Cell cell) {
        if (cell == null || cell.getType() != CellType.NUMBER) {
            return false;
        }
        BigDecimal value = cell.asNumber();
        if (value == null || value.signum() < 0) {
            return false;
        }
        Integer formatId = cell.getDataFormatId();
        if (formatId == null) {
            return false;
        }
        if ((formatId >= 0x0e && formatId <= 0x16) || (formatId >= 0x2d && formatId <= 0x2f)) {
            return true;
        }
        return isDateFormatString(cell.getDataFormatString());
    }

    private static String formatNumericCell(Cell cell) {
        BigDecimal number = cell.asNumber();
        if (number == null) {
            return "";
        }
        if (isDateFormatted(cell)) {
            String formatted = formatDateCell(cell);
            if (formatted != null) {
                return formatted;
            }
        }
        String formatted = formatWithSimpleNumberPattern(number.doubleValue(), cell.getDataFormatString());
        return formatted != null ? formatted : formatGeneralNumber(number.doubleValue());
    }

    /** Excel「常规」格式：≥1E11 或 ≤1E-10 用科学计数，整数原样，其余保留 10 位有效数字。 */
    private static String formatGeneralNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.getDefault());
        double abs = Math.abs(value);
        if (abs >= 1E11 || (abs <= 1E-10 && abs > 0)) {
            DecimalFormat scientific = new DecimalFormat("0.#####E0", symbols);
            scientific.setRoundingMode(RoundingMode.HALF_UP);
            String text = scientific.format(value);
            int e = text.indexOf('E');
            if (e > 0 && e + 1 < text.length() && text.charAt(e + 1) != '-') {
                text = text.substring(0, e + 1) + "+" + text.substring(e + 1);
            }
            return text;
        }
        if (Math.floor(value) == value || abs >= 1E10) {
            DecimalFormat integer = new DecimalFormat("#", symbols);
            integer.setRoundingMode(RoundingMode.HALF_UP);
            return integer.format(value);
        }
        double rounded = new BigDecimal(value).round(new MathContext(10, RoundingMode.HALF_UP)).doubleValue();
        DecimalFormat decimal = new DecimalFormat("#.##########", symbols);
        decimal.setRoundingMode(RoundingMode.HALF_UP);
        return decimal.format(rounded);
    }

    /** 仅处理单段、只含 {@code 0#,.%} 的数字格式（如 0.00、#,##0、0%）；其余返回 null 走「常规」格式。 */
    private static String formatWithSimpleNumberPattern(double value, String pattern) {
        if (StringUtils.isEmpty(pattern) || !SIMPLE_NUMBER_PATTERN.matcher(pattern).matches()) {
            return null;
        }
        try {
            DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.getDefault()));
            format.setRoundingMode(RoundingMode.HALF_UP);
            return format.format(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String formatDateCell(Cell cell) {
        LocalDateTime dateTime = cell.asDate();
        if (dateTime == null) {
            return null;
        }
        Integer formatId = cell.getDataFormatId();
        String excelFormat = formatId != null && BUILTIN_DATE_FORMATS.containsKey(formatId)
                ? BUILTIN_DATE_FORMATS.get(formatId) : cell.getDataFormatString();
        String javaPattern = toJavaDatePattern(excelFormat);
        if (javaPattern == null) {
            return null;
        }
        try {
            // 按日历字段构造（而非 Instant 换算），避免 1900 年前的时区历史偏移让时间漂移
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(dateTime.getYear(), dateTime.getMonthValue() - 1, dateTime.getDayOfMonth(),
                    dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
            calendar.set(Calendar.MILLISECOND, dateTime.getNano() / 1_000_000);
            return new SimpleDateFormat(javaPattern, Locale.getDefault()).format(calendar.getTime());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Excel 日期格式 → {@link SimpleDateFormat} 模式（对齐 POI DataFormatter 的转换规则：m 在 h 之后或 s 之前为分钟）。 */
    private static String toJavaDatePattern(String excelFormat) {
        if (StringUtils.isEmpty(excelFormat)) {
            return null;
        }
        String format = excelFormat.replace(";@", "");
        int separator = indexOfUnquoted(format, ';');
        if (separator >= 0) {
            format = format.substring(0, separator);
        }
        boolean hasAmPm = format.toUpperCase(Locale.ROOT).contains("AM/PM")
                || format.toUpperCase(Locale.ROOT).contains("A/P");
        format = format.replaceAll("(?i)AM/PM", "@").replaceAll("(?i)A/P", "@");
        StringBuilder sb = new StringBuilder();
        List<Integer> monthIndexes = new ArrayList<>();
        boolean mIsMonth = true;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (c == '"') {
                int end = format.indexOf('"', i + 1);
                String literal = end < 0 ? format.substring(i + 1) : format.substring(i + 1, end);
                // 与 POI DataFormatter 一致：引号文本连同引号一起输出
                appendLiteral(sb, "\"" + literal + "\"");
                i = end < 0 ? format.length() : end;
            } else if (c == '\\' && i + 1 < format.length()) {
                appendLiteral(sb, String.valueOf(format.charAt(++i)));
            } else if (c == '[') {
                int end = format.indexOf(']', i + 1);
                String inner = end < 0 ? "" : format.substring(i + 1, end).toLowerCase(Locale.ROOT);
                if (inner.matches("h+")) {
                    sb.append('H');
                    mIsMonth = false;
                } else if (inner.matches("m+")) {
                    sb.append('m');
                } else if (inner.matches("s+")) {
                    sb.append('s');
                }
                i = end < 0 ? format.length() : end;
            } else if (c == '@') {
                sb.append('a');
            } else if (c == 'h' || c == 'H') {
                mIsMonth = false;
                sb.append(hasAmPm ? 'h' : 'H');
            } else if (c == 'm' || c == 'M') {
                if (mIsMonth) {
                    sb.append('M');
                    monthIndexes.add(sb.length() - 1);
                } else {
                    sb.append('m');
                }
            } else if (c == 's' || c == 'S') {
                sb.append('s');
                for (int index : monthIndexes) {
                    if (sb.charAt(index) == 'M') {
                        sb.setCharAt(index, 'm');
                    }
                }
                mIsMonth = true;
                monthIndexes.clear();
            } else if (Character.isLetter(c)) {
                mIsMonth = true;
                monthIndexes.clear();
                if (c == 'y' || c == 'Y') {
                    sb.append('y');
                } else if (c == 'd' || c == 'D') {
                    sb.append('d');
                } else {
                    appendLiteral(sb, String.valueOf(c));
                }
            } else {
                if (Character.isWhitespace(c)) {
                    monthIndexes.clear();
                }
                if (c == '\'') {
                    sb.append("''");
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void appendLiteral(StringBuilder sb, String literal) {
        if (literal.isEmpty()) {
            return;
        }
        sb.append('\'').append(literal.replace("'", "''")).append('\'');
    }

    private static int indexOfUnquoted(String text, char target) {
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == '\\') {
                i++;
            } else if (c == target && !quoted) {
                return i;
            }
        }
        return -1;
    }

    /** 对齐 POI {@code DateUtil#isADateFormat}。 */
    private static boolean isDateFormatString(String formatString) {
        if (StringUtils.isEmpty(formatString)) {
            return false;
        }
        StringBuilder sb = new StringBuilder(formatString.length());
        for (int i = 0; i < formatString.length(); i++) {
            char c = formatString.charAt(i);
            if (i < formatString.length() - 1) {
                char next = formatString.charAt(i + 1);
                if (c == '\\' && (next == '-' || next == ',' || next == '.' || next == ' ' || next == '\\')) {
                    continue;
                }
                if (c == ';' && next == '@') {
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        String fs = sb.toString();
        if (DATE_ELAPSED_PATTERN.matcher(fs).matches()) {
            return true;
        }
        fs = DATE_DBNUM_PATTERN.matcher(fs).replaceAll("");
        fs = DATE_LOCALE_PATTERN.matcher(fs).replaceAll("");
        fs = DATE_COLOR_PATTERN.matcher(fs).replaceAll("");
        int separatorIndex = fs.indexOf(';');
        if (0 < separatorIndex && separatorIndex < fs.length() - 1) {
            fs = fs.substring(0, separatorIndex);
        }
        if (!DATE_CHAR_PATTERN.matcher(fs).find()) {
            return false;
        }
        return DATE_FORMAT_PATTERN.matcher(fs).matches();
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

    public static String formatExportValue(Object value, SysProjectDefectField def) {
        if (value == null || isEmptyValue(value)) {
            return "";
        }
        String type = def.getFieldType();
        if ("enum".equals(type)) {
            Map<String, Object> cfg = def.getTypeConfig() != null ? def.getTypeConfig() : Map.of();
            List<?> options = asList(cfg.get("options"));
            String sk = String.valueOf(value);
            if (options != null) {
                for (Object item : options) {
                    Map<?, ?> opt = asMap(item);
                    if (opt != null && sk.equals(String.valueOf(opt.get("key")))) {
                        Object label = opt.get("label");
                        return label != null ? String.valueOf(label) : sk;
                    }
                }
            }
            return sk;
        }
        if ("boolean".equals(type)) {
            if (value instanceof Boolean bool) {
                return bool ? "true" : "false";
            }
            return String.valueOf(value);
        }
        if ("image".equals(type) || "file".equals(type) || "array".equals(type)) {
            if (value instanceof List<?> list) {
                return list.stream().map(String::valueOf).collect(Collectors.joining(", "));
            }
            return String.valueOf(value);
        }
        if ("object".equals(type)) {
            if (value instanceof Map<?, ?> || value instanceof JsonNode) {
                try {
                    return MAPPER.writeValueAsString(value);
                } catch (Exception ignored) {
                    return String.valueOf(value);
                }
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
            Map<String, Object> cfg = def.getTypeConfig() != null ? def.getTypeConfig() : Map.of();
            List<?> options = asList(cfg.get("options"));
            if (options != null) {
                for (Object item : options) {
                    Map<?, ?> opt = asMap(item);
                    if (opt == null) {
                        continue;
                    }
                    String key = opt.get("key") != null ? String.valueOf(opt.get("key")) : null;
                    String label = opt.get("label") != null ? String.valueOf(opt.get("label")) : null;
                    if (text.equals(key) || (label != null && text.equals(label))) {
                        return key;
                    }
                }
            }
            return text;
        }
        if ("boolean".equals(type)) {
            Boolean bool = DefectCustomFieldsValidator.coerceBoolean(text);
            return bool != null ? bool : text;
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

    /** 导入模版布尔列下拉：与导出文本一致的 true/false。 */
    public static String[] resolveBooleanComboLabels() {
        return new String[] {"true", "false"};
    }

    public static String[] resolveEnumComboLabels(SysProjectDefectField def) {
        if (def == null || !"enum".equals(def.getFieldType())) {
            return new String[0];
        }
        Map<String, Object> cfg = def.getTypeConfig() != null ? def.getTypeConfig() : Map.of();
        List<?> options = asList(cfg.get("options"));
        if (options == null || options.isEmpty()) {
            return new String[0];
        }
        List<String> labels = new ArrayList<>();
        for (Object item : options) {
            Map<?, ?> opt = asMap(item);
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

    public static Set<String> buildStandardImportHeaderTexts() {
        Set<String> set = new HashSet<>();
        set.addAll(ExcelI18n.allTranslations("type"));
        set.addAll(ExcelI18n.allTranslations("defect.name"));
        set.addAll(ExcelI18n.allTranslations("describe"));
        set.addAll(ExcelI18n.allTranslations("defect.level"));
        set.addAll(ExcelI18n.allTranslations("defect.state"));
        set.addAll(ExcelI18n.allTranslations("module"));
        set.addAll(ExcelI18n.allTranslations("version"));
        set.addAll(ExcelI18n.allTranslations("image"));
        set.addAll(ExcelI18n.allTranslations("handle-by"));
        set.addAll(ExcelI18n.allTranslations("id"));
        return set;
    }

    private static boolean isCustomRequired(JsonNode col) {
        return col != null && col.path("required").asBoolean(false);
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
            return MAPPER.valueToTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return StringUtils.isEmpty(text) ? null : text;
    }

    private static Map<Integer, SysProjectDefectField> mapCustomColumnsByHeader(List<Cell> header,
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
        for (int c = 0; c < header.size(); c++) {
            Cell cell = header.get(c);
            if (cell == null) {
                continue;
            }
            String headerText = formatCellText(cell).trim();
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

    private static int findHeaderRowIndex(List<List<Cell>> rows) {
        for (int r = 0; r <= Math.min(5, rows.size() - 1); r++) {
            for (Cell cell : rows.get(r)) {
                if (cell != null && StringUtils.isNotEmpty(formatCellText(cell))) {
                    return r;
                }
            }
        }
        return 0;
    }

    private static boolean isEmptyRow(List<Cell> row) {
        for (Cell cell : row) {
            if (cell != null && StringUtils.isNotEmpty(formatCellText(cell).trim())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str) {
            return StringUtils.isBlank(str);
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof JsonNode node && node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(item -> list.add(MAPPER.convertValue(item, Map.class)));
            return list;
        }
        return null;
    }

    private static Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        if (value instanceof JsonNode node && node.isObject()) {
            return MAPPER.convertValue(node, Map.class);
        }
        return null;
    }
}
