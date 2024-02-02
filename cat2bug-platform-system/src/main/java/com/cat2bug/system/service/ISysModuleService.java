package com.cat2bug.system.service;

import java.util.List;
import java.util.Set;

import com.cat2bug.system.domain.SysModule;

/**
 * 模块Service接口
 * 
 * @author yuzhantao
 * @date 2023-11-26
 */
public interface ISysModuleService 
{
    /**
     * 获取所有子模块Id
     * @param moduleId  父模块id
     * @return  子模块集合
     */
    public Set<Long> getAllChildIds(Long moduleId);
    /**
     * 查询模块
     * 
     * @param moduleId 模块主键
     * @return 模块
     */
    public SysModule selectSysModuleByModuleId(Long moduleId);

    /**
     * 查询模块列表
     * 
     * @param sysModule 模块
     * @return 模块集合
     */
    public List<SysModule> selectSysModuleList(SysModule sysModule);

    /**
     * 查询模块路径列表
     *
     * @param projectId 模块
     * @return 模块路径集合
     */
    public List<SysModule> selectSysModulePathList(Long projectId);

    /**
     * 新增模块
     * 
     * @param sysModule 模块
     * @return 结果
     */
    public int insertSysModule(SysModule sysModule);

    /**
     * 修改模块
     * 
     * @param sysModule 模块
     * @return 结果
     */
    public int updateSysModule(SysModule sysModule);

    /**
     * 批量删除模块
     * 
     * @param moduleIds 需要删除的模块主键集合
     * @return 结果
     */
    public int deleteSysModuleByModuleIds(Long[] moduleIds);

    /**
     * 删除模块信息
     * 
     * @param moduleId 模块主键
     * @return 结果
     */
    public int deleteSysModuleByModuleId(Long moduleId);
}
