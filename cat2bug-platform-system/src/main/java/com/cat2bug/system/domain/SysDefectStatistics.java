package com.cat2bug.system.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-21 15:33
 * @Version: 1.0.0
 * 缺陷统计
 */
@Data
public class SysDefectStatistics {
    /** 总数量 */
    private int total;
    /** 待处理数量 */
    private int pendingCount;
    /** 待验证数量 */
    private int verifyCount;
    /** 已关闭数量 */
    private int closedCount;
}
