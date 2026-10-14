package com.cat2bug.system.open;

import com.cat2bug.common.domain.open.ApiMember;
import com.cat2bug.system.mapper.OpenApiMemberMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class OpenApiMemberService {

    @Inject
    OpenApiMemberMapper openApiMemberMapper;

    public List<ApiMember> selectMemberList(Long projectId, ApiMember query) {
        return openApiMemberMapper.selectMemberList(projectId, query != null ? query : new ApiMember());
    }
}
