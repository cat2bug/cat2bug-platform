package com.cat2bug.api.service;

import com.cat2bug.api.domain.ApiCase;

import java.util.List;

/**
 * 测试用例服务
 */
public interface IApiCaseService {
    public List<ApiCase> selectApiCaseList(ApiCase apiCase);

    /**
     * 查询单条用例（当前 API Key 对应项目、用例编号），字段与 {@link #selectApiCaseList} 行一致
     */
    ApiCase selectApiCaseByCaseNum(Long caseNum);
}
