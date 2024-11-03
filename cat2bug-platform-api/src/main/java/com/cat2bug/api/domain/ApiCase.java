package com.cat2bug.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * 测试用例对象 sys_case
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
@Data
public class ApiCase
{
    private static final long serialVersionUID = 1L;

    /** 用例号码 */
    private Long caseNum;

    /** 用例名称 */
    private String caseName;

    /** 预期 */
    private String caseExpect;

    /** 步骤 */
    private List<ApiCaseStep> caseStep;

    /** 前置条件 */
    private String casePreconditions;

    /** 数据 */
    private String caseData;

    /** 交付物ID */
    @JsonIgnore
    private Long deliverableId;

    /** 交付物名称 */
    private String deliverableName;

    /** 备注 */
    private String remark;

    /** 用例级别 */
    private Long caseLevel;

    /** 图片集合 */
    private List<String> imgUrlList;
}
