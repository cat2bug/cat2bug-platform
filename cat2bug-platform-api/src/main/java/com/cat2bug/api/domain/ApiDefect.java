package com.cat2bug.api.domain;

import com.cat2bug.api.domain.type.ApiDefectStateEnum;
import com.cat2bug.api.domain.type.ApiDefectTypeEnum;
import com.cat2bug.common.core.domain.entity.SysDefect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 缺陷对象
 */
@Data
public class ApiDefect
{
    /** 缺陷id */
    @JsonIgnore
    private Long defectId;

    /** 缺陷编号 */
    private Long defectNum;

    /** 缺陷标题 */
    private String defectName;

    /** 缺陷类型 */
    private ApiDefectTypeEnum defectType;

    /** 缺陷状态 */
    private ApiDefectStateEnum defectState;

    /** 缺陷等级 */
    private String defectLevel;

    /** 交付物ID */
    @JsonIgnore
    private Long deliverableId;

    /** 交付物名称 */
    private String deliverableName;

    /** 版本 */
    private String version;

    /** 创建人 */
    private ApiMemberBaseInfo creator;

    /** 处理人名称列表 */
    private List<ApiMemberBaseInfo> handlerList;

    /** 图片列表 */
    private List<String> imgUrlList;

    /** 附件 */
    private List<String> annexUrlList;

    /** 缺陷Key */
    private String defectKey;
    /** 缺陷组Key */
    private String defectGroupKey;

    /** 缺陷描述 */
    private String defectDescribe;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 档期日志 */
    private ApiDefectLog currentLog;

    /** 更新人ID */
    @JsonIgnore
    private Long updateById;
    /** 更新人 */
    @JsonIgnore
    private String updateBy;

    /** 创建人ID */
    @JsonIgnore
    private Long createById;
    /** 创建人 */
    @JsonIgnore
    private String createBy;
    /** 创建时间 */
    @JsonIgnore
    private Date createTime;
    /** 项目ID */
    @JsonIgnore
    private Long projectId;
}
