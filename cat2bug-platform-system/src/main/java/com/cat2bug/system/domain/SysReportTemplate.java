package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 报告模版对象 sys_report_template
 * 
 * @author yuzhantao
 * @date 2024-04-29
 */
public class SysReportTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 报告模版ID */
    private Long templateId;

    /** 交付物类型 */
    @Excel(name = "交付物类型")
    private String moduleType;

    /** 模版内容 */
    @Excel(name = "模版内容")
    private String templateContent;

    /** 模版标题 */
    @Excel(name = "模版标题")
    private String templateTitle;

    /** 更新用户 */
    @Excel(name = "更新用户")
    private Long updateById;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 模版图标路径 */
    @Excel(name = "模版图标路径")
    private String templateIconUrl;

    /** 主版本 */
    @Excel(name = "主版本")
    private Long majorVersion;

    /** 次版本 */
    @Excel(name = "次版本")
    private Long minorVersion;

    /** 模版唯一标识 */
    @Excel(name = "模版唯一标识")
    private String templateKey;

    public void setTemplateId(Long templateId) 
    {
        this.templateId = templateId;
    }

    public Long getTemplateId() 
    {
        return templateId;
    }
    public void setModuleType(String moduleType) 
    {
        this.moduleType = moduleType;
    }

    public String getModuleType() 
    {
        return moduleType;
    }
    public void setTemplateContent(String templateContent) 
    {
        this.templateContent = templateContent;
    }

    public String getTemplateContent() 
    {
        return templateContent;
    }
    public void setUpdateById(Long updateById) 
    {
        this.updateById = updateById;
    }

    public Long getUpdateById() 
    {
        return updateById;
    }
    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }
    public void setTemplateIconUrl(String templateIconUrl) 
    {
        this.templateIconUrl = templateIconUrl;
    }

    public String getTemplateIconUrl() 
    {
        return templateIconUrl;
    }
    public void setMajorVersion(Long majorVersion) 
    {
        this.majorVersion = majorVersion;
    }

    public Long getMajorVersion() 
    {
        return majorVersion;
    }
    public void setMinorVersion(Long minorVersion) 
    {
        this.minorVersion = minorVersion;
    }

    public Long getMinorVersion() 
    {
        return minorVersion;
    }
    public void setTemplateKey(String templateKey) 
    {
        this.templateKey = templateKey;
    }

    public String getTemplateKey() 
    {
        return templateKey;
    }

    public String getTemplateTitle() {
        return templateTitle;
    }

    public void setTemplateTitle(String templateTitle) {
        this.templateTitle = templateTitle;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("templateId", getTemplateId())
            .append("moduleType", getModuleType())
            .append("templateContent", getTemplateContent())
            .append("updateById", getUpdateById())
            .append("updateTime", getUpdateTime())
            .append("projectId", getProjectId())
            .append("templateIconUrl", getTemplateIconUrl())
            .append("majorVersion", getMajorVersion())
            .append("minorVersion", getMinorVersion())
            .append("templateKey", getTemplateKey())
            .toString();
    }
}
