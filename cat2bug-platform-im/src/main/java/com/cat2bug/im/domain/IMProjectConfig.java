package com.cat2bug.im.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 项目通知配置对象 im_project_config
 * 
 * @author yuzhantao
 * @date 2024-07-27
 */
@Data
public class IMProjectConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 项目ID */
    private Long projectId;

    /** 第三方系统编码 */
    private String systemCode;

    /** 配置参数 */
    private String configParams;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("projectId", getProjectId())
            .append("systemCode", getSystemCode())
            .append("configParams", getConfigParams())
            .toString();
    }
}
