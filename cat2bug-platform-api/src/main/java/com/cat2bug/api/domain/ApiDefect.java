package com.cat2bug.api.domain;

import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import com.cat2bug.api.domain.type.ApiDefectTypeEnum;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 缺陷对象
 */
@Data
public class ApiDefect extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 缺陷id */
    private Long defectId;

    /** 缺陷编号 */
    private Long defectNumber;

    private ApiDefectTypeEnum defectType;

    /** 缺陷标题 */
    private String defectName;

    /** 缺陷描述 */
    private String defectDescribe;

    /** 附件 */
    private String annexList;

    /** 图片列表 */
    private String imgList;

    /** 项目名称 */
    private String projectName;

    /** 测试模块名称 */
    private String moduleName;

    /** 版本 */
    private String moduleVersion;

    /** 缺陷状态 */
    @Excel(name = "缺陷状态")
    private ApiDefectStateEnum defectState;

    /** 处理人id */
    private List<String> handleByList;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date handleTime;

    /** 缺陷等级 */
    private String defectLevel;

    @Excel(name = "缺陷等级")
    private String defectLevelName;
}
