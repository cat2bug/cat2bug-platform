package com.cat2bug.im.service;

import java.util.List;
import com.cat2bug.im.domain.IMProjectConfig;

/**
 * 项目通知配置Service接口
 * 
 * @author yuzhantao
 * @date 2024-07-27
 */
public interface IIMProjectConfigService
{
    /**
     * 查询项目通知配置
     * 
     * @param configId 项目通知配置主键
     * @return 项目通知配置
     */
    public IMProjectConfig selectImProjectConfigByConfigId(Long configId);

    /**
     * 查询项目通知配置
     * @param selectImProjectConfigByProjectId 项目ID
     * @param systemCode    平台代码
     * @return 项目通知配置
     */
    public IMProjectConfig selectImProjectConfigByProjectIdAndSystemCode(Long selectImProjectConfigByProjectId,String systemCode);

    /**
     * 查询项目通知配置列表
     * 
     * @param imProjectConfig 项目通知配置
     * @return 项目通知配置集合
     */
    public List<IMProjectConfig> selectImProjectConfigList(IMProjectConfig imProjectConfig);

    /**
     * 新增项目通知配置
     * 
     * @param imProjectConfig 项目通知配置
     * @return 结果
     */
    public int insertImProjectConfig(IMProjectConfig imProjectConfig);

    /**
     * 修改项目通知配置
     * 
     * @param imProjectConfig 项目通知配置
     * @return 结果
     */
    public int updateImProjectConfig(IMProjectConfig imProjectConfig);

    /**
     * 批量删除项目通知配置
     * 
     * @param configIds 需要删除的项目通知配置主键集合
     * @return 结果
     */
    public int deleteImProjectConfigByConfigIds(Long[] configIds);

    /**
     * 删除项目通知配置信息
     * 
     * @param configId 项目通知配置主键
     * @return 结果
     */
    public int deleteImProjectConfigByConfigId(Long configId);
}
