package com.cat2bug.api.domain;

import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import com.cat2bug.api.domain.type.ApiDefectTypeEnum;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    /** 项目ID */
    @JsonIgnoreProperties
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    @JsonIgnoreProperties
    private Long moduleId;

    /** 测试模块名称 */
    private String moduleName;

    /** 版本 */
    private String moduleVersion;

    /** 缺陷状态 */
    @Excel(name = "缺陷状态")
    private ApiDefectStateEnum defectState;

    /** 处理人id */
    @JsonIgnoreProperties
    private List<Long> handleBy;

    /** 处理人名称列表 */
    private List<String> handleByList;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date handleTime;

    /** 缺陷等级 */
    private String defectLevel;

    /** 缺陷等级 */
    @Excel(name = "缺陷等级")
    private String defectLevelName;

    /** 缺陷Key */
    private String defectKey;
    /** 缺陷组Key */
    private String defectGroupKey;
}
