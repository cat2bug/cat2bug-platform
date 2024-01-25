package com.cat2bug.system.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 用户统计模版对象 sys_user_statistic_template
 * 
 * @author yuzhantao
 * @date 2024-01-24
 */
@Data
public class SysUserStatisticTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 统计模版id */
    private Long statisticTemplateId;

    /** 统计模版编码 */
    @Excel(name = "统计模版编码")
    private String statisticTemplatCode;

    /** 模型类型 */
    @Excel(name = "模型类型")
    private Long moduleType;

    /** 项目id */
    @Excel(name = "项目id")
    private Long projectId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 统计模版配置 */
    @Excel(name = "统计模版配置")
    private String statisticTemplatConfig;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("statisticTemplateId", getStatisticTemplateId())
            .append("statisticTemplatCode", getStatisticTemplatCode())
            .append("moduleType", getModuleType())
            .append("projectId", getProjectId())
            .append("userId", getUserId())
            .append("statisticTemplatConfig", getStatisticTemplatConfig())
            .toString();
    }
}
