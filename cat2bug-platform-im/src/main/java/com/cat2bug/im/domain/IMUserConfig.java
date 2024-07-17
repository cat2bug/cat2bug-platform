package com.cat2bug.im.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 用户消息配置对象 im_user_config
 * 
 * @author yuzhantao
 * @date 2024-07-18
 */
@Data
public class IMUserConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 信息配置ID */
    private Long imConfigId;

    /** 成员ID */
    @Excel(name = "成员ID")
    @JsonProperty("memberId")
    private Long userId;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 模块类型 */
    @Excel(name = "模块类型")
    private String groupName;

    /** 配置项 */
    @Excel(name = "配置项")
    private IMConfig config;


    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("imConfigId", getImConfigId())
            .append("userId", getUserId())
            .append("projectId", getProjectId())
            .append("groupName", getGroupName())
            .append("config", getConfig())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
