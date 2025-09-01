package com.cat2bug.api.service.impl;

import com.cat2bug.api.domain.ApiMember;

import java.util.List;

/**
 * 测试用例服务
 */
public interface IApiMemberService {
    public List<ApiMember> selectApiMemberList(ApiMember apiMember);
}
