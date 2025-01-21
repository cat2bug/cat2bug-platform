package com.cat2bug.system.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-21 15:33
 * @Version: 1.0.0
 * 测试用例统计
 */
@Data
public class SysMemberStatistics {
    /** 总数量 */
    private int total;
    /** 管理员数量 */
    private int adminCount;
    /** 测试数量 */
    private int testCount;
    /** 开发数量 */
    private int developmentCount;
    /** 外部人员数量 */
    private int outsideCount;
}
