package com.cat2bug.common.constant;

import com.cat2bug.common.domain.vo.EnumVo;

import java.util.ArrayList;
import java.util.List;

/**
 * 缺陷类型/状态常量（与 Spring {@code SysDefectTypeEnum}/{@code SysDefectStateEnum} 对齐）。
 */
public final class DefectConstants {

    private DefectConstants() {
    }

    public static final String[] DEFECT_TYPES = {"BUG", "TASK", "DEMAND"};

    public static final String[] DEFECT_STATES = {"PROCESSING", "AUDIT", "RESOLVED", "REJECTED", "CLOSED"};

    public static final int STATE_PROCESSING = 0;
    public static final int STATE_AUDIT = 1;
    public static final int STATE_RESOLVED = 2;
    public static final int STATE_REJECTED = 3;
    public static final int STATE_CLOSED = 4;

    public static String typeName(Integer code) {
        return nameOf(DEFECT_TYPES, code);
    }

    public static String stateName(Integer code) {
        return nameOf(DEFECT_STATES, code);
    }

    /** 名字（不区分大小写）或数字 → 序号；空返回 null；非法抛 {@link IllegalArgumentException}。 */
    public static Integer parseType(String value) {
        return parse(DEFECT_TYPES, value, "缺陷类型");
    }

    public static Integer parseState(String value) {
        return parse(DEFECT_STATES, value, "缺陷状态");
    }

    private static String nameOf(String[] names, Integer code) {
        return code != null && code >= 0 && code < names.length ? names[code] : null;
    }

    private static Integer parse(String[] names, String value, String kind) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.chars().allMatch(Character::isDigit)) {
            int code;
            try {
                code = Integer.parseInt(v);
            } catch (NumberFormatException e) {
                code = -1;
            }
            if (code >= 0 && code < names.length) {
                return code;
            }
        } else {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(v)) {
                    return i;
                }
            }
        }
        throw new IllegalArgumentException(kind + "取值应为 " + String.join(" / ", names) + " 或 0～"
                + (names.length - 1) + "：" + value);
    }

    public static List<EnumVo> getDefectTypeList() {
        List<EnumVo> list = new ArrayList<>();
        for (int i = 0; i < DEFECT_TYPES.length; i++) {
            list.add(new EnumVo(i, DEFECT_TYPES[i]));
        }
        return list;
    }

    public static List<EnumVo> getDefectStateList() {
        List<EnumVo> list = new ArrayList<>();
        for (int i = 0; i < DEFECT_STATES.length; i++) {
            if (!"RESOLVED".equals(DEFECT_STATES[i])) {
                list.add(new EnumVo(i, DEFECT_STATES[i]));
            }
        }
        return list;
    }
}
