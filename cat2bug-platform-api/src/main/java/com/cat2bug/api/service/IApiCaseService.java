package com.cat2bug.api.service;

import com.cat2bug.api.domain.ApiCase;

import java.util.List;

/**
 * 测试用例服务
 */
public interface IApiCaseService {
    public List<ApiCase> selectApiCaseList(ApiCase apiCase);
}
