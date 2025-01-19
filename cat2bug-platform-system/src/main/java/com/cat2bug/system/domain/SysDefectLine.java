package com.cat2bug.system.domain;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-19 04:40
 * @Version: 1.0.0
 */
@Data
public class SysDefectLine {
    private long defectCount;
    private String defectTime;
    private SysDefectStateEnum defectState;
}
