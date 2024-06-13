package com.cat2bug.web.vo;

import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-13 23:09
 * @Version: 1.0.0
 */
@Data
public class AiDefect {
    /** 缺陷类型 */
    private String defectType;

    /** 缺陷标题 */
    private String defectName;

    /** 测试模块id */
    private Long moduleId;

    /** 处理人id */
    private Long handleBy;

    /** 缺陷等级 */
    private String defectLevel;
}
