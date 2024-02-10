package com.cat2bug.api.service;

import com.cat2bug.api.domain.ApiDefect;

import java.util.List;

public interface IApiDefectService {
    /**
     * 查询缺陷列表
     *
     * @param apiDefect 缺陷
     * @return 缺陷集合
     */
    public List<ApiDefect> selectApiDefectList(ApiDefect apiDefect);

    /**
     * 新增缺陷
     *
     * @param apiDefect 缺陷
     * @return 结果
     */
    public ApiDefect insertApiDefect(ApiDefect apiDefect);
}
