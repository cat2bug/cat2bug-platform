package com.cat2bug.quarkus.common.util;

import com.cat2bug.quarkus.common.domain.SysDefect;

/**
 * 缺陷类型、优先级、状态的持久化默认值。
 */
public final class DefectDefaults {

    public static final int DEFAULT_TYPE = 0;
    public static final String DEFAULT_LEVEL = "middle";
    public static final int DEFAULT_STATE = 0;

    private DefectDefaults() {
    }

    public static void applyDefectDefaults(SysDefect defect) {
        if (defect == null) {
            return;
        }
        if (defect.getDefectType() == null) {
            defect.setDefectType(DEFAULT_TYPE);
        }
        if (StringUtils.isEmpty(defect.getDefectLevel())) {
            defect.setDefectLevel(DEFAULT_LEVEL);
        }
        if (defect.getDefectState() == null) {
            defect.setDefectState(DEFAULT_STATE);
        }
    }
}
