package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * AI模型配置对象 sys_ai_module_config
 * 
 * @author yuzhantao
 * @date 2024-06-20
 */
public class SysAiModuleConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模型ID */
    private Long aiId;

    /** 业务模型名称 */
    @Excel(name = "业务模型名称")
    private String businessModule;

    /** 图片识别模型名称 */
    @Excel(name = "图片识别模型名称")
    private String imageModule;

    /** 项目ID */
    @Excel(name = "项目ID")
    private Long projectId;

    /** 创建人ID */
    private Long createById;

    /** 更新人ID */
    private Long updateById;

    public void setAiId(Long aiId) 
    {
        this.aiId = aiId;
    }

    public Long getAiId() 
    {
        return aiId;
    }
    public void setBusinessModule(String businessModule) 
    {
        this.businessModule = businessModule;
    }

    public String getBusinessModule() 
    {
        return businessModule;
    }
    public void setImageModule(String imageModule) 
    {
        this.imageModule = imageModule;
    }

    public String getImageModule() 
    {
        return imageModule;
    }
    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }
    public void setCreateById(Long createById) 
    {
        this.createById = createById;
    }

    public Long getCreateById() 
    {
        return createById;
    }
    public void setUpdateById(Long updateById) 
    {
        this.updateById = updateById;
    }

    public Long getUpdateById() 
    {
        return updateById;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("aiId", getAiId())
            .append("businessModule", getBusinessModule())
            .append("imageModule", getImageModule())
            .append("projectId", getProjectId())
            .append("createTime", getCreateTime())
            .append("createById", getCreateById())
            .append("updateTime", getUpdateTime())
            .append("updateById", getUpdateById())
            .toString();
    }
}
