package com.cat2bug.system.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysCase;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.domain.SysProjectDefectFieldColumnLayout;
import com.cat2bug.system.mapper.SysCaseMapper;
import com.cat2bug.system.service.ISysProjectDefectFieldService;
import com.cat2bug.system.util.DefectBuiltinFieldRegistry.BuiltinDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知内缺陷属性列表：顺序与显隐对齐缺陷处理抽屉「基本信息」（字段管理 + 显示字段启用项）。
 */
@Component
public class DefectNoticeFieldSupport {

    /** 与前端 DEFECT_DETAIL_SKIP_TABLE_KEYS 对应（标题/独立描述区已展示） */
    private static final Set<String> DETAIL_SKIP_FIELD_KEYS = new HashSet<>(Arrays.asList(
            "projectNum", "defectName", "defectDescribe"));

    /** 与详情页风箱列一致：行内列表不展示，仅格网字段 */
    private static final Set<String> INLINE_SKIP_BUILTIN_KEYS = new HashSet<>(Arrays.asList(
            "imgUrls", "annexUrls"));

    private static final Set<String> INLINE_SKIP_CUSTOM_TYPES = new HashSet<>(Arrays.asList(
            "image", "object", "file"));

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private ISysProjectDefectFieldService projectDefectFieldService;

    @Autowired
    private SysCaseMapper sysCaseMapper;

    public static final class FieldLine {
        private final String label;
        private final String value;

        public FieldLine(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

    public List<FieldLine> buildFieldLines(SysDefect defect) {
        if (defect == null || defect.getProjectId() == null) {
            return new ArrayList<>();
        }
        SysProjectDefectFieldColumnLayout layout =
                projectDefectFieldService.selectColumnLayoutByProjectId(defect.getProjectId());
        return buildFieldLines(defect, layout);
    }

    List<FieldLine> buildFieldLines(SysDefect defect, SysProjectDefectFieldColumnLayout layout) {
        List<FieldLine> lines = new ArrayList<>();
        if (defect == null || layout == null) {
            return lines;
        }
        Map<String, BuiltinDef> builtinDefs = DefectBuiltinFieldRegistry.definitionByKey();
        Set<String> enabledBuiltin = new HashSet<>(
                layout.getEnabledBuiltinFieldKeys() != null ? layout.getEnabledBuiltinFieldKeys() : new ArrayList<>());
        Map<String, SysProjectDefectField> customByKey = new LinkedHashMap<>();
        if (layout.getCustomFields() != null) {
            for (SysProjectDefectField field : layout.getCustomFields()) {
                if (field != null && StringUtils.isNotBlank(field.getFieldKey())) {
                    customByKey.put(field.getFieldKey(), field);
                }
            }
        }

        List<String> orderedKeys = layout.getOrderedEnabledFieldKeys();
        if (orderedKeys == null || orderedKeys.isEmpty()) {
            orderedKeys = new ArrayList<>();
            for (BuiltinDef def : DefectBuiltinFieldRegistry.definitions()) {
                if (enabledBuiltin.contains(def.fieldKey)) {
                    orderedKeys.add(def.fieldKey);
                }
            }
            if (layout.getCustomFields() != null) {
                layout.getCustomFields().stream()
                        .filter(f -> f != null && StringUtils.isNotBlank(f.getFieldKey()))
                        .map(SysProjectDefectField::getFieldKey)
                        .forEach(orderedKeys::add);
            }
        }

        Set<String> seen = new HashSet<>();
        for (String fieldKey : orderedKeys) {
            if (StringUtils.isBlank(fieldKey) || seen.contains(fieldKey) || DETAIL_SKIP_FIELD_KEYS.contains(fieldKey)) {
                continue;
            }
            if (builtinDefs.containsKey(fieldKey)) {
                if (!enabledBuiltin.contains(fieldKey)
                        && !DefectBuiltinFieldRegistry.isAlwaysEnabled(fieldKey)) {
                    continue;
                }
                if (INLINE_SKIP_BUILTIN_KEYS.contains(fieldKey)) {
                    continue;
                }
                BuiltinDef def = builtinDefs.get(fieldKey);
                String label = resolveBuiltinLabel(fieldKey, def);
                String value = formatBuiltinValue(defect, fieldKey);
                seen.add(fieldKey);
                lines.add(new FieldLine(label, value));
                continue;
            }
            SysProjectDefectField custom = customByKey.get(fieldKey);
            if (custom == null || INLINE_SKIP_CUSTOM_TYPES.contains(custom.getFieldType())) {
                continue;
            }
            seen.add(fieldKey);
            lines.add(new FieldLine(
                    resolveCustomLabel(custom),
                    formatCustomValue(defect, custom)));
        }

        lines.add(new FieldLine(
                MessageUtils.message("defect.life-time"),
                formatLifeTime(defect)));
        lines.add(new FieldLine(
                MessageUtils.message("reject"),
                String.valueOf(defect.getRejectCount())));
        return lines;
    }

    private static String resolveBuiltinLabel(String fieldKey, BuiltinDef def) {
        String key = "defect.builtin-field." + fieldKey;
        String label = MessageUtils.message(key);
        if (StringUtils.isNotBlank(label) && !key.equals(label)) {
            return label;
        }
        if (def != null && StringUtils.isNotBlank(def.labelKey)) {
            return MessageUtils.message(def.labelKey);
        }
        return fieldKey;
    }

    private static String resolveCustomLabel(SysProjectDefectField field) {
        if (field != null && StringUtils.isNotBlank(field.getFieldLabel())) {
            return field.getFieldLabel();
        }
        return field != null ? field.getFieldKey() : "";
    }

    private String formatBuiltinValue(SysDefect defect, String fieldKey) {
        switch (fieldKey) {
            case "defectType":
                return defect.getDefectType() != null
                        ? MessageUtils.message(defect.getDefectType().name()) : "";
            case "defectState":
                return defect.getDefectState() != null
                        ? MessageUtils.message(defect.getDefectState().name()) : "";
            case "defectLevel":
                return defect.getDefectLevel() != null
                        ? MessageUtils.message(defect.getDefectLevel()) : "";
            case "handleBy":
                return formatMemberNames(defect.getHandleByList());
            case "moduleId":
                return resolveModuleDisplay(defect);
            case "moduleVersion":
                return StringUtils.toNotBlankValue(defect.getModuleVersion());
            case "caseId":
                return resolveCaseDisplay(defect);
            case "createMember":
                if (StringUtils.isNotBlank(defect.getCreateMemberName())) {
                    return defect.getCreateMemberName();
                }
                SysUser creator = defect.getCreateMember();
                return creator != null
                        ? StringUtils.toNotBlankValue(
                        StringUtils.isNotBlank(creator.getNickName()) ? creator.getNickName() : creator.getUserName())
                        : "";
            case "planStartTime":
            case "planEndTime":
            case "updateTime":
            case "createTime":
                return formatDate(readDateField(defect, fieldKey));
            default:
                return "";
        }
    }

    private Date readDateField(SysDefect defect, String fieldKey) {
        switch (fieldKey) {
            case "planStartTime":
                return defect.getPlanStartTime();
            case "planEndTime":
                return defect.getPlanEndTime();
            case "updateTime":
                return defect.getUpdateTime();
            case "createTime":
                return defect.getCreateTime();
            default:
                return null;
        }
    }

    private String resolveModuleDisplay(SysDefect defect) {
        if (StringUtils.isNotBlank(defect.getModulePath())) {
            return defect.getModulePath().trim();
        }
        return StringUtils.toNotBlankValue(defect.getModuleName());
    }

    private String resolveCaseDisplay(SysDefect defect) {
        if (defect.getCaseId() == null) {
            return "";
        }
        SysCase caseItem = sysCaseMapper.selectSysCaseByCaseId(defect.getCaseId());
        if (caseItem != null && StringUtils.isNotBlank(caseItem.getCaseName())) {
            return caseItem.getCaseName();
        }
        return String.valueOf(defect.getCaseId());
    }

    private static String formatMemberNames(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return "";
        }
        return users.stream()
                .map(u -> {
                    if (u == null) {
                        return "";
                    }
                    if (StringUtils.isNotBlank(u.getNickName())) {
                        return u.getNickName();
                    }
                    return StringUtils.toNotBlankValue(u.getUserName());
                })
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    private String formatCustomValue(SysDefect defect, SysProjectDefectField field) {
        Map<String, Object> customFields = defect.getCustomFields();
        if (customFields == null || field == null) {
            return "";
        }
        Object value = customFields.get(field.getFieldKey());
        if (value == null) {
            return "";
        }
        String type = field.getFieldType();
        if ("enum".equals(type)) {
            return resolveEnumLabel(field, String.valueOf(value));
        }
        if ("boolean".equals(type)) {
            return Boolean.TRUE.equals(value) ? MessageUtils.message("yes") : MessageUtils.message("no");
        }
        if ("datetime".equals(type)) {
            return String.valueOf(value);
        }
        if ("array".equals(type) && value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                parts.add(String.valueOf(arr.get(i)));
            }
            return String.join(", ", parts);
        }
        if (value instanceof List) {
            return ((List<?>) value).stream().map(String::valueOf).collect(Collectors.joining(", "));
        }
        return String.valueOf(value);
    }

    private static String resolveEnumLabel(SysProjectDefectField field, String key) {
        if (field == null || field.getTypeConfig() == null || StringUtils.isBlank(key)) {
            return key != null ? key : "";
        }
        Object options = field.getTypeConfig().get("options");
        if (!(options instanceof List)) {
            return key;
        }
        for (Object item : (List<?>) options) {
            if (item instanceof Map) {
                Object optionKey = ((Map<?, ?>) item).get("key");
                if (key.equals(String.valueOf(optionKey))) {
                    Object label = ((Map<?, ?>) item).get("label");
                    return label != null ? String.valueOf(label) : key;
                }
            } else if (item instanceof JSONObject) {
                JSONObject obj = (JSONObject) item;
                if (key.equals(obj.getString("key"))) {
                    String label = obj.getString("label");
                    return StringUtils.isNotBlank(label) ? label : key;
                }
            }
        }
        return key;
    }

    static String formatLifeTime(SysDefect defect) {
        if (defect == null) {
            return "";
        }
        Date passTime = defect.getHandlePassTime() != null ? defect.getHandlePassTime() : new Date();
        Date startTime = defect.getHandleStartTime() != null ? defect.getHandleStartTime() : new Date();
        long diff = Math.max(0L, passTime.getTime() - startTime.getTime());
        long day = diff / (1000L * 60 * 60 * 24);
        diff %= (1000L * 60 * 60 * 24);
        long hour = diff / (1000L * 60 * 60);
        diff %= (1000L * 60 * 60);
        long minute = diff / (1000L * 60);
        List<String> parts = new ArrayList<>();
        if (day > 0) {
            parts.add(day + MessageUtils.message("day"));
        }
        if (hour > 0) {
            parts.add(hour + MessageUtils.message("hour"));
        }
        parts.add(minute + MessageUtils.message("minute"));
        return String.join(" ", parts);
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
