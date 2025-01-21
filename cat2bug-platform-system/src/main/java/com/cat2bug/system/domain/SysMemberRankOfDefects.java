package com.cat2bug.system.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2025-01-20 22:14
 * @Version: 1.0.0
 */
@Data
public class SysMemberRankOfDefects {
    /** 成员姓名 */
    private String nickName;
    /** 成员登陆名 */
    private String userName;
    /** 成员头显 */
    private String avatar;
    /** 缺陷处理总数量 */
    private int defectTotal;
    /** 今日缺陷处理数量 */
    private int defectTodayCount;
}
