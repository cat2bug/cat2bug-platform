package com.cat2bug.system.domain;

import lombok.Data;

/**
 * 当前用户未关闭待办汇总
 */
@Data
public class SysDefectOpenWorkloadSummary {
    private int total;
    private int processing;
    private int audit;
    private int rejected;
}
