package com.cat2bug.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 测试计划倒计时统计块所需字段
 */
@Data
public class SysPlanCountdownSummary {
    private String planId;
    private String planName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date planEndTime;

    private int itemTotal;
    private int unexecutedCount;
    private int passCount;
    private int defectCount;
    private int defectCloseStateCount;
}
