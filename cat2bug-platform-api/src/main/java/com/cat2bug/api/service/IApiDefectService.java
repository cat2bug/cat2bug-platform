package com.cat2bug.api.service;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.domain.ApiDefectHandle;
import com.cat2bug.api.domain.ApiDefectRequest;
import com.cat2bug.system.domain.SysDefectLog;

import java.util.List;

public interface IApiDefectService {
    /**
     * 查询缺陷列表
     *
     * @param apiDefect 缺陷
     * @return 缺陷集合
     */
    public List<ApiDefect> selectApiDefectList(Long projectId, ApiDefectRequest apiDefect);

    /**
     * 查询缺陷
     * @param number    缺陷编号
     * @return  缺陷
     */
    public ApiDefect selectSysDefectByDefectNumber(Long number);

    /**
     * 新增或更新缺陷
     *
     * @param apiDefect 缺陷
     * @return 结果
     */
    public ApiDefect insertOrUpdateApiDefect(ApiDefectRequest apiDefect);

    /**
     * 新增缺陷
     *
     * @param apiDefect 缺陷
     * @return 结果
     */
    public ApiDefect insertApiDefect(ApiDefectRequest apiDefect);

    /**
     * 更新缺陷
     * @param apiDefect 缺陷
     * @return 结果
     */
    public ApiDefect updateSysDefect(ApiDefectRequest apiDefect);
    /**
     * 指派
     * @param apiDefectHandle 缺陷处理
     * @return  缺陷
     */
    public ApiDefect assign(ApiDefectHandle apiDefectHandle);

    /**
     * 驳回
     * @param apiDefectHandle  缺陷处理
     * @return  缺陷
     */
    public ApiDefect reject(ApiDefectHandle apiDefectHandle);

    /**
     * 修复
     * @param apiDefectHandle  缺陷处理
     * @return  缺陷
     */
    public ApiDefect repair(ApiDefectHandle apiDefectHandle);

    /**
     * 通过
     * @param apiDefectHandle  缺陷处理
     * @return  缺陷
     */
    public ApiDefect pass(ApiDefectHandle apiDefectHandle);

    /**
     * 关闭
     * @param apiDefectHandle  缺陷处理
     * @return  缺陷
     */
    public ApiDefect close(ApiDefectHandle apiDefectHandle);

    /**
     * 启动
     * @param apiDefectHandle  缺陷处理
     * @return  缺陷
     */
    public ApiDefect open(ApiDefectHandle apiDefectHandle);
}
