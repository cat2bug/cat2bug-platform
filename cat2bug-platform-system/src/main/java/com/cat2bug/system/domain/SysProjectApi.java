package com.cat2bug.system.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 项目API对象 sys_project_api
 * 
 * @author yuzhantao
 * @date 2024-02-11
 */
@Data
public class SysProjectApi extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** API_KEY */
    private String apiId;

    /** 项目id */
    @Excel(name = "项目id")
    private Long projectId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 白名单 */
    @Excel(name = "白名单")
    private List<String> whiteList;

    /** 有效时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireTime;

    /** API名称 */
    @Excel(name = "API名称")
    private String apiName;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("apiId", getApiId())
            .append("projectId", getProjectId())
            .append("userId", getUserId())
            .append("whiteList", getWhiteList())
            .append("expireTime", getExpireTime())
            .append("remark", getRemark())
            .append("apiName", getApiName())
            .toString();
    }
}
