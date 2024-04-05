package com.cat2bug.common.core.domain.entity;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 报告对象 sys_report
 * 
 * @author yuzhantao
 * @date 2024-03-13
 */
@Data
public class SysReport<T> extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 报告ID */
    private Long reportId;

    /** 报告标题 */
    @Excel(name = "报告标题")
    private String reportTitle;

    /** 报告时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:ss:mm")
    @Excel(name = "报告时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date reportTime;

    /** 报告描述 */
    @Excel(name = "报告描述")
    private String reportDescription;

    /** 数据类型编解码器 */
    private String reportDataCoder;

    /** 数据 */
    @Excel(name = "数据")
    private T reportData;

    /** 数据来源 */
    @Excel(name = "数据来源")
    private String reportSource;

    /**  推送人ID */
    @Excel(name = " 推送人ID")
    private Long createById;

    /**  项目ID */
    private Long projectId;

    /** 处理人 */
    private String handler;

    /** 焦点成员 */
    private List<SysUser> focusList;
    /** 报告KEY */
    private String reportKey;
}
