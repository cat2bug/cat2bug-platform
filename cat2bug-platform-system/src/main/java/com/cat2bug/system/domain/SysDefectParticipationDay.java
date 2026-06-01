package com.cat2bug.system.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 按日缺陷日志参与统计
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SysDefectParticipationDay {
    /** yyyy-MM-dd */
    private String date;
    private int count;
}
