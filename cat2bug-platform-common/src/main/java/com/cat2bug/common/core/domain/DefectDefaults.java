package com.cat2bug.common.core.domain;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.StringUtils;

/**
 * 缺陷类型、优先级、状态的持久化默认值。
 */
public final class DefectDefaults
{
    public static final SysDefectTypeEnum DEFAULT_TYPE = SysDefectTypeEnum.BUG;

    public static final String DEFAULT_LEVEL = "middle";

    public static final SysDefectStateEnum DEFAULT_STATE = SysDefectStateEnum.PROCESSING;

    private DefectDefaults()
    {
    }

    /**
     * 将缺失的类型、优先级、状态补为默认值；不覆盖已有合法值。
     */
    public static void applyDefectDefaults(SysDefect defect)
    {
        if (defect == null)
        {
            return;
        }
        if (defect.getDefectType() == null)
        {
            defect.setDefectType(DEFAULT_TYPE);
        }
        if (StringUtils.isBlank(defect.getDefectLevel()))
        {
            defect.setDefectLevel(DEFAULT_LEVEL);
        }
        if (defect.getDefectState() == null)
        {
            defect.setDefectState(DEFAULT_STATE);
        }
    }
}
