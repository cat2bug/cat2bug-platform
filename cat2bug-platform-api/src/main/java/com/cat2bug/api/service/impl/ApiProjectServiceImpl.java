package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.api.mapper.ApiProjectMapper;
import com.cat2bug.api.service.IApiProjectService;
import com.cat2bug.system.domain.SysProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * API项目信息Service业务层处理
 *
 * @author yuzhantao
 * @date 2026-04-09
 */
@Service
public class ApiProjectServiceImpl implements IApiProjectService {

    @Autowired
    private ApiProjectMapper apiProjectMapper;

    /**
     * 根据项目ID查询项目信息
     *
     * @param projectId 项目ID
     * @return 项目信息
     */
    @Override
    public SysProject selectProjectById(Long projectId) {
        return apiProjectMapper.selectProjectById(projectId);
    }

    /**
     * 根据项目ID查询创建成员信息
     *
     * @param projectId 项目ID
     * @return 创建成员信息
     */
    @Override
    public ApiMember selectCreatorByProjectId(Long projectId) {
        return apiProjectMapper.selectCreatorByProjectId(projectId);
    }

    /**
     * 获取项目统计信息
     *
     * @param projectId 项目ID
     * @return 统计信息
     */
    @Override
    public Map<String, Object> getProjectStatistics(Long projectId) {
        return apiProjectMapper.getProjectStatistics(projectId);
    }
}
