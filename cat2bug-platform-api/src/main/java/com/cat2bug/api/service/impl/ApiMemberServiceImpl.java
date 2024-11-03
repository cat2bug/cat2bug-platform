package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiMember;
import com.cat2bug.api.mapper.ApiMemberMapper;
import com.cat2bug.api.service.ApiService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 15:25
 * @Version: 1.0.0
 */
@Service
public class ApiMemberServiceImpl implements IApiMemberService {
    @Resource
    private ApiMemberMapper apiMemberMapper;
    @Resource
    private ApiService apiService;
    @Override
    public List<ApiMember> selectApiMemberList(ApiMember apiMember) {
        Long projectId = this.apiService.getProjectId();
        return this.apiMemberMapper.selectMemberList(projectId, apiMember);
    }
}
