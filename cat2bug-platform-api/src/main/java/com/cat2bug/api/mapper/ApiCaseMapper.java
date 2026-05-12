package com.cat2bug.api.mapper;

import com.cat2bug.api.domain.ApiCase;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测试用例Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-01-28
 */
public interface ApiCaseMapper
{

    /**
     * 查询测试用例列表
     *
     * @param projectId 项目ID
     * @param apiCase 测试用例
     * @return 测试用例集合
     */
    public List<ApiCase> selectApiCaseList(@Param("projectId") Long projectId, @Param("apiCase") ApiCase apiCase);

    /**
     * 按用例编号查询单条（限定项目），用于 Open API 详情，与列表行结构一致
     */
    ApiCase selectApiCaseByCaseNum(@Param("projectId") Long projectId, @Param("caseNum") Long caseNum);
}
