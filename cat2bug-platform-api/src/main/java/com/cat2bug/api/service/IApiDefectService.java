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
     * 查询缺陷
     * @param number    缺陷编号
     * @return  缺陷
     */
    public ApiDefect selectSysDefectByDefectNumber(Long number);

    /**
     * 新增缺陷
     *
     * @param apiDefect 缺陷
     * @return 结果
     */
    public ApiDefect insertApiDefect(ApiDefect apiDefect);
}
