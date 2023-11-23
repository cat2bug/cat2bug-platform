package com.cat2bug.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 缺陷对象 sys_defect
 * 
 * @author yuzhantao
 * @date 2023-11-23
 */
public class SysDefect extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 缺陷id
 */
    private Long defectId;

    /** 缺陷类型
 */
    @Excel(name = "缺陷类型")
    private Integer defectType;

    /** 缺陷标题 */
    @Excel(name = "缺陷标题")
    private String defectName;

    /** 缺陷描述 */
    private String defectDescribe;

    /** 附件 */
    @Excel(name = "附件")
    private String annexUrls;

    /** 项目id */
    @Excel(name = "项目id")
    private Long projectId;

    /** 测试计划id */
    @Excel(name = "测试计划id")
    private Long testPlanId;

    /** 测试用例id */
    @Excel(name = "测试用例id")
    private Long caseId;

    /** 数据来源 */
    @Excel(name = "数据来源")
    private Integer dataSources;

    /** 数据来源相关参数 */
    @Excel(name = "数据来源相关参数")
    private String dataSourcesParams;

    /** 测试模块id */
    @Excel(name = "测试模块id")
    private Long moduleId;

    /** 版本 */
    @Excel(name = "版本")
    private String moduleVersion;

    /** 缺陷状态 */
    @Excel(name = "缺陷状态")
    private Integer defectState;

    /** 用例步骤id */
    @Excel(name = "用例步骤id")
    private Long caseStepId;

    /** 处理人id */
    @Excel(name = "处理人id")
    private String handleBy;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "处理时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date handleTime;

    /** 缺陷等级 */
    @Excel(name = "缺陷等级")
    private String defectLevel;

    public void setDefectId(Long defectId) 
    {
        this.defectId = defectId;
    }

    public Long getDefectId() 
    {
        return defectId;
    }
    public void setDefectType(Integer defectType) 
    {
        this.defectType = defectType;
    }

    public Integer getDefectType() 
    {
        return defectType;
    }
    public void setDefectName(String defectName) 
    {
        this.defectName = defectName;
    }

    public String getDefectName() 
    {
        return defectName;
    }
    public void setDefectDescribe(String defectDescribe) 
    {
        this.defectDescribe = defectDescribe;
    }

    public String getDefectDescribe() 
    {
        return defectDescribe;
    }
    public void setAnnexUrls(String annexUrls) 
    {
        this.annexUrls = annexUrls;
    }

    public String getAnnexUrls() 
    {
        return annexUrls;
    }
    public void setProjectId(Long projectId) 
    {
        this.projectId = projectId;
    }

    public Long getProjectId() 
    {
        return projectId;
    }
    public void setTestPlanId(Long testPlanId) 
    {
        this.testPlanId = testPlanId;
    }

    public Long getTestPlanId() 
    {
        return testPlanId;
    }
    public void setCaseId(Long caseId) 
    {
        this.caseId = caseId;
    }

    public Long getCaseId() 
    {
        return caseId;
    }
    public void setDataSources(Integer dataSources) 
    {
        this.dataSources = dataSources;
    }

    public Integer getDataSources() 
    {
        return dataSources;
    }
    public void setDataSourcesParams(String dataSourcesParams) 
    {
        this.dataSourcesParams = dataSourcesParams;
    }

    public String getDataSourcesParams() 
    {
        return dataSourcesParams;
    }
    public void setModuleId(Long moduleId) 
    {
        this.moduleId = moduleId;
    }

    public Long getModuleId() 
    {
        return moduleId;
    }
    public void setModuleVersion(String moduleVersion) 
    {
        this.moduleVersion = moduleVersion;
    }

    public String getModuleVersion() 
    {
        return moduleVersion;
    }
    public void setDefectState(Integer defectState) 
    {
        this.defectState = defectState;
    }

    public Integer getDefectState() 
    {
        return defectState;
    }
    public void setCaseStepId(Long caseStepId) 
    {
        this.caseStepId = caseStepId;
    }

    public Long getCaseStepId() 
    {
        return caseStepId;
    }
    public void setHandleBy(String handleBy) 
    {
        this.handleBy = handleBy;
    }

    public String getHandleBy() 
    {
        return handleBy;
    }
    public void setHandleTime(Date handleTime) 
    {
        this.handleTime = handleTime;
    }

    public Date getHandleTime() 
    {
        return handleTime;
    }
    public void setDefectLevel(String defectLevel) 
    {
        this.defectLevel = defectLevel;
    }

    public String getDefectLevel() 
    {
        return defectLevel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("defectId", getDefectId())
            .append("defectType", getDefectType())
            .append("defectName", getDefectName())
            .append("defectDescribe", getDefectDescribe())
            .append("annexUrls", getAnnexUrls())
            .append("projectId", getProjectId())
            .append("testPlanId", getTestPlanId())
            .append("caseId", getCaseId())
            .append("dataSources", getDataSources())
            .append("dataSourcesParams", getDataSourcesParams())
            .append("moduleId", getModuleId())
            .append("moduleVersion", getModuleVersion())
            .append("createBy", getCreateBy())
            .append("updateTime", getUpdateTime())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("defectState", getDefectState())
            .append("caseStepId", getCaseStepId())
            .append("handleBy", getHandleBy())
            .append("handleTime", getHandleTime())
            .append("defectLevel", getDefectLevel())
            .toString();
    }
}
