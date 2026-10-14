package com.cat2bug.system.util;

import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.SysDictData;
import com.cat2bug.common.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 缺陷 Excel 导入：将模板中的类型/状态/等级显示名解析为持久化值。
 */
public final class DefectImportLabelResolver {

    private DefectImportLabelResolver() {
    }

    public static Integer resolveType(String label) {
        if (StringUtils.isEmpty(label)) {
            return null;
        }
        String trimmed = label.trim();
        for (int i = 0; i < DefectConstants.DEFECT_TYPES.length; i++) {
            if (DefectConstants.DEFECT_TYPES[i].equalsIgnoreCase(trimmed)) {
                return i;
            }
        }
        return null;
    }

    public static Integer resolveState(String label) {
        if (StringUtils.isEmpty(label)) {
            return null;
        }
        String trimmed = label.trim();
        for (int i = 0; i < DefectConstants.DEFECT_STATES.length; i++) {
            if (DefectConstants.DEFECT_STATES[i].equalsIgnoreCase(trimmed)) {
                return i;
            }
        }
        return null;
    }

    public static String resolveLevel(String label, List<SysDictData> dictData) {
        if (StringUtils.isEmpty(label)) {
            return null;
        }
        String trimmed = label.trim();
        if (dictData != null) {
            for (SysDictData dict : dictData) {
                if (trimmed.equals(dict.getDictValue())) {
                    return dict.getDictValue();
                }
                if (dict.getDictLabel() != null && trimmed.equals(dict.getDictLabel().trim())) {
                    return dict.getDictValue();
                }
            }
        }
        return levelByLabel(dictData).get(trimmed);
    }

    public static String formatType(Integer defectType) {
        if (defectType == null || defectType < 0 || defectType >= DefectConstants.DEFECT_TYPES.length) {
            return "";
        }
        return DefectConstants.DEFECT_TYPES[defectType];
    }

    public static String formatState(Integer defectState) {
        if (defectState == null || defectState < 0 || defectState >= DefectConstants.DEFECT_STATES.length) {
            return "";
        }
        return DefectConstants.DEFECT_STATES[defectState];
    }

    public static String formatLevel(String defectLevel, List<SysDictData> dictData) {
        if (StringUtils.isEmpty(defectLevel)) {
            return "";
        }
        if (dictData != null) {
            for (SysDictData dict : dictData) {
                if (defectLevel.equals(dict.getDictValue())) {
                    return dict.getDictLabel() != null ? dict.getDictLabel() : dict.getDictValue();
                }
            }
        }
        return defectLevel;
    }

    private static Map<String, String> levelByLabel(List<SysDictData> dictData) {
        Map<String, String> map = new HashMap<>();
        if (dictData == null) {
            return map;
        }
        for (SysDictData dict : dictData) {
            if (StringUtils.isEmpty(dict.getDictValue())) {
                continue;
            }
            map.put(dict.getDictValue().trim(), dict.getDictValue());
            if (dict.getDictLabel() != null) {
                map.put(dict.getDictLabel().trim(), dict.getDictValue());
            }
            map.put(dict.getDictValue().trim().toLowerCase(Locale.ROOT), dict.getDictValue());
        }
        return map;
    }
}
