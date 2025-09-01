package com.cat2bug.system.service;

import java.util.List;
import com.cat2bug.system.domain.SysProjectApi;

/**
 * 项目APIService接口
 * 
 * @author yuzhantao
 * @date 2024-02-11
 */
public interface ISysProjectApiService 
{
    /**
     * 查询项目API
     * 
     * @param apiId 项目API主键
     * @return 项目API
     */
    public SysProjectApi selectSysProjectApiByApiId(String apiId);

    /**
     * 查询项目API列表
     * 
     * @param sysProjectApi 项目API
     * @return 项目API集合
     */
    public List<SysProjectApi> selectSysProjectApiList(SysProjectApi sysProjectApi);

    /**
     * 新增项目API
     * 
     * @param sysProjectApi 项目API
     * @return 结果
     */
    public int insertSysProjectApi(SysProjectApi sysProjectApi);

    /**
     * 修改项目API
     * 
     * @param sysProjectApi 项目API
     * @return 结果
     */
    public int updateSysProjectApi(SysProjectApi sysProjectApi);

    /**
     * 批量删除项目API
     * 
     * @param apiIds 需要删除的项目API主键集合
     * @return 结果
     */
    public int deleteSysProjectApiByApiIds(String[] apiIds);

    /**
     * 删除项目API信息
     * 
     * @param apiId 项目API主键
     * @return 结果
     */
    public int deleteSysProjectApiByApiId(String apiId);
}
