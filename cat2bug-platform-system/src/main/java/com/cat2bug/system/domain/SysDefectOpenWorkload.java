package com.cat2bug.system.domain;

import lombok.Data;

/**
 * 成员未关闭待办负载（团队排行）
 */
@Data
public class SysDefectOpenWorkload {
    private Long userId;
    private String nickName;
    private String avatar;
    private int total;
    private int processing;
    private int audit;
    private int rejected;
}
