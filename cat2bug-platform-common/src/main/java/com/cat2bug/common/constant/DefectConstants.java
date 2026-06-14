package com.cat2bug.common.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 缺陷类型/状态常量（与 {@code SysDefectTypeEnum}/{@code SysDefectStateEnum} 对齐，B9 FastExcel 模版用）。
 */
public final class DefectConstants {

    private DefectConstants() {
    }

    public static final String[] DEFECT_TYPES = {"BUG", "TASK", "DEMAND"};

    public static final String[] DEFECT_STATES = {"PROCESSING", "AUDIT", "RESOLVED", "REJECTED", "CLOSED"};

    public static List<String> defectTypeLabels() {
        return Arrays.asList(DEFECT_TYPES);
    }

    public static List<String> defectStateLabels() {
        return Arrays.asList(DEFECT_STATES);
    }
}
