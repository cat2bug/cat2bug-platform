package com.cat2bug.quarkus.common.constant;

import com.cat2bug.quarkus.common.domain.vo.EnumVo;

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
