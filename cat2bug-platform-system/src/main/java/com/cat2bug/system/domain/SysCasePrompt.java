package com.cat2bug.system.domain;

import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.cat2bug.common.annotation.Excel;
import com.cat2bug.common.core.domain.BaseEntity;

/**
 * 测试用例提示词对象 sys_case_prompt
 * 
 * @author yuzhantao
 * @date 2024-10-15
 */
@Data
public class SysCasePrompt extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 测试用例提示词ID */
    private String casePromptId;

    /** 提示词内容 */
    @Excel(name = "提示词内容")
    private String casePromptContent;

    /** 项目ID */
    private Long projectId;

    /** 创建人ID */
    @Excel(name = "创建人ID")
    private Long createById;


    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("casePromptId", getCasePromptId())
            .append("casePromptContent", getCasePromptContent())
            .append("projectId", getProjectId())
            .append("createById", getCreateById())
            .append("createTime", getCreateTime())
            .toString();
    }
}
