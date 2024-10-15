package com.cat2bug.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 测试计划子项对象 sys_plan_item
 * 
 * @author yuzhantao
 * @date 2024-10-11
 */
public class SysPlanItem extends SysCase
{
    private static final long serialVersionUID = 1L;

    /** 测试计划子项ID */
    private String planItemId;

    /** 测试计划 */
    @Excel(name = "测试计划")
    private String planId;

    /** 交付物 */
    @Excel(name = "交付物")
    private Long moduleId;

    /** 测试用例 */
    @Excel(name = "测试用例")
    private Long caseId;

    /** 计划子项状态 */
    @Excel(name = "计划子项状态")
    private String planItemState;

    /** 更新人 */
    @Excel(name = "更新人")
    private Long updateById;

    /** 缺陷 */
    @Excel(name = "缺陷")
    private Long defectId;

    public void setPlanItemId(String planItemId) 
    {
        this.planItemId = planItemId;
    }

    public String getPlanItemId() 
    {
        return planItemId;
    }
    public void setPlanId(String planId) 
    {
        this.planId = planId;
    }

    public String getPlanId() 
    {
        return planId;
    }
    public void setModuleId(Long moduleId) 
    {
        this.moduleId = moduleId;
    }

    public Long getModuleId() 
    {
        return moduleId;
    }
    public void setCaseId(Long caseId) 
    {
        this.caseId = caseId;
    }

    public Long getCaseId() 
    {
        return caseId;
    }
    public void setPlanItemState(String planItemState) 
    {
        this.planItemState = planItemState;
    }

    public String getPlanItemState() 
    {
        return planItemState;
    }
    public void setUpdateById(Long updateById) 
    {
        this.updateById = updateById;
    }

    public Long getUpdateById() 
    {
        return updateById;
    }
    public void setDefectId(Long defectId) 
    {
        this.defectId = defectId;
    }

    public Long getDefectId() 
    {
        return defectId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("planItemId", getPlanItemId())
            .append("planId", getPlanId())
            .append("moduleId", getModuleId())
            .append("caseId", getCaseId())
            .append("planItemState", getPlanItemState())
            .append("updateById", getUpdateById())
            .append("updateTime", getUpdateTime())
            .append("defectId", getDefectId())
            .toString();
    }
}
