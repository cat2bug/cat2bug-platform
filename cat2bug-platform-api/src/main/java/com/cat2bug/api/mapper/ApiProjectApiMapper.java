package com.cat2bug.api.mapper;


import com.cat2bug.api.domain.ApiProjectApi;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目APIMapper接口
 * 
 * @author yuzhantao
 * @date 2024-02-11
 */
@Mapper
public interface ApiProjectApiMapper
{
    /**
     * 查询项目id
     * 
     * @param apiId 项目API主键
     * @return 项目id
     */
    public Long selectProjectIdByApiId(String apiId);
}
