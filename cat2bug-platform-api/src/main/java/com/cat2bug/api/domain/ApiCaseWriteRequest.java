package com.cat2bug.api.domain;

import lombok.Data;

import java.util.List;

/**
 * Open API 创建/更新测试用例请求体（使用交付物全路径名称等；更新时仅用 {@link #caseNum} 定位，不使用 caseId）
 */
@Data
public class ApiCaseWriteRequest {
    /** 更新时必填：项目内用例编号，与列表/详情返回的 caseNum 一致；服务端据此查询，不使用 caseId */
    private Long caseNum;
    private String caseName;
    private String caseExpect;
    private List<ApiCaseStep> caseStep;
    private Long caseLevel;
    private String casePreconditions;
    private String caseData;
    /** 交付物全路径，多级用 {@code /} 分隔；与列表筛选、响应字段同名 */
    private String deliverableName;
    private String remark;
}
