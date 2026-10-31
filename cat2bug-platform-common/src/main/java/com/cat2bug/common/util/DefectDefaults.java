package com.cat2bug.common.util;

import com.cat2bug.common.constant.DefectConstants;
import com.cat2bug.common.domain.SysDefect;

import java.util.List;

/**
 * 缺陷类型、优先级、状态的持久化默认值与「已创建 / 处理中」规范化。
 */
public final class DefectDefaults {

    public static final int DEFAULT_TYPE = 0;
    public static final String DEFAULT_LEVEL = "middle";
    public static final int DEFAULT_STATE = DefectConstants.STATE_PROCESSING;

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
            defect.setDefectState(hasHandler(defect)
                    ? DefectConstants.STATE_PROCESSING
                    : DefectConstants.STATE_CREATED);
        }
    }

    /**
     * 非通过/关闭终态下：无人 → CREATED；CREATED 且有人 → PROCESSING。
     */
    public static void normalizeCreatedProcessingByHandler(SysDefect defect) {
        if (defect == null) {
            return;
        }
        Integer state = defect.getDefectState();
        if (state != null
                && (state == DefectConstants.STATE_CLOSED || state == DefectConstants.STATE_RESOLVED)) {
            return;
        }
        if (!hasHandler(defect)) {
            defect.setDefectState(DefectConstants.STATE_CREATED);
        } else if (state == null || state == DefectConstants.STATE_CREATED) {
            defect.setDefectState(DefectConstants.STATE_PROCESSING);
        }
    }

    public static boolean hasHandler(SysDefect defect) {
        if (defect == null) {
            return false;
        }
        List<Long> handleBy = defect.getHandleBy();
        return handleBy != null && !handleBy.isEmpty();
    }
}
