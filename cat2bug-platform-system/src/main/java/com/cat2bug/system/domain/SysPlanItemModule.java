package com.cat2bug.system.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-10-23 19:18
 * @Version: 1.0.0
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class SysPlanItemModule extends SysModule {
    private long itemCount;
    private String planId;
    private long passCount;
    private long defectCount;
}
