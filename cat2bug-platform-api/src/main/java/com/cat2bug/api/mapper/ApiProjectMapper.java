package com.cat2bug.api.mapper;

import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.system.domain.SysProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * API项目信息Mapper接口
 *
 * @author yuzhantao
 * @date 2026-04-09
 */
@Mapper
public interface ApiProjectMapper {

    /**
     * 根据项目ID查询项目信息
     *
     * @param projectId 项目ID
     * @return 项目信息
     */
    SysProject selectProjectById(@Param("projectId") Long projectId);

    /**
     * 根据项目ID查询创建成员信息
     *
     * @param projectId 项目ID
     * @return 创建成员信息
     */
    ApiMember selectCreatorByProjectId(@Param("projectId") Long projectId);

    /**
     * 获取项目统计信息
     *
     * @param projectId 项目ID
     * @return 统计信息
     */
    Map<String, Object> getProjectStatistics(@Param("projectId") Long projectId);
}
