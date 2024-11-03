package com.cat2bug.api.domain;

import com.cat2bug.api.domain.type.ApiDefectLogStateEnum;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;

/**
 * 缺陷日志对象 sys_defect_log
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
@Data
public class ApiDefectLog
{
    /** 缺陷日志ID */
    @JsonIgnore
    private Long defectLogId;
    /** 关联缺陷ID */
    @JsonIgnore
    private Long defectId;
    /** 缺陷日志类型 */
    private ApiDefectLogStateEnum defectLogState;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /** 创建人ID */
    @JsonIgnore
    private Long createById;
    /** 创建人 */
    @JsonIgnore
    private String createBy;
    /** 接收处理人 */
    @JsonIgnore
    private List<ApiMember> receiverList;
    /** 接收处理人ID集合 */
    @JsonIgnore
    private List<Long> receiveBy;
    /** 缺陷日志的描述 */
    private String defectLogDescribe;
}
