package com.cat2bug.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 测试计划质量指标（雷达图）
 */
@Data
public class SysPlanMetricsItem {
    private String planId;
    private String planName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private double discovery;
    private double repair;
    private double detection;
    private double severity;
    private double restart;
    private double escape;
}
