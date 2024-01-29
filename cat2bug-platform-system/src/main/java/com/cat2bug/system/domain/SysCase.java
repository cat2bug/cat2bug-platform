package com.cat2bug.system.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

import java.util.List;

/**
 * 测试用例对象 sys_case
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
@Data
public class SysCase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 测试用例 */
    private Long caseId;

    /** 用例名称 */
    @Excel(name = "用例名称")
    private String caseName;

    /** 模块名称 */
    @Excel(name = "模块")
    private String moduleName;

    /** 模块id */
    private Long moduleId;

    /** 用例类型 */
    @Excel(name = "用例类型")
    private Long caseType;

    /** 预期 */
    @Excel(name = "预期")
    private String caseExpect;

    /** 步骤 */
    @Excel(name = "步骤")
    private List<SysCaseStep> caseStep;

    /** 用例级别 */
    @Excel(name = "用例级别")
    private Long caseLevel;

    /** 前置条件 */
    @Excel(name = "前置条件")
    private String casePreconditions;

    /** 用例号码 */
    @Excel(name = "用例号码")
    private Long caseNum;

    /** 项目编号 */
    @Excel(name = "项目编号")
    private Long projectId;

    /** 备注 */
    private String remark;

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("caseId", getCaseId())
            .append("caseName", getCaseName())
            .append("moduleId", getModuleId())
            .append("caseType", getCaseType())
            .append("caseExpect", getCaseExpect())
            .append("caseStep", getCaseStep())
            .append("caseLevel", getCaseLevel())
            .append("casePreconditions", getCasePreconditions())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("caseNum", getCaseNum())
            .append("projectId", getProjectId())
            .toString();
    }
}
