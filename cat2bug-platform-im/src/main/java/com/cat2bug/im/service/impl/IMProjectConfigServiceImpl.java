package com.cat2bug.im.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.im.mapper.IMProjectConfigMapper;
import com.cat2bug.im.domain.IMProjectConfig;
import com.cat2bug.im.service.IIMProjectConfigService;

/**
 * 项目通知配置Service业务层处理
 * 
 * @author yuzhantao
 * @date 2024-07-27
 */
@Service
public class IMProjectConfigServiceImpl implements IIMProjectConfigService
{
    @Autowired
    private IMProjectConfigMapper imProjectConfigMapper;

    /**
     * 查询项目通知配置
     * 
     * @param configId 项目通知配置主键
     * @return 项目通知配置
     */
    @Override
    public IMProjectConfig selectImProjectConfigByConfigId(Long configId)
    {
        return imProjectConfigMapper.selectImProjectConfigByConfigId(configId);
    }

    /**
     * 查询项目通知配置
     * @param projectId 项目ID
     * @param systemCode    平台编码
     * @return 项目通知配置
     */
    @Override
    public IMProjectConfig selectImProjectConfigByProjectIdAndSystemCode(Long projectId,String systemCode) {
        return imProjectConfigMapper.selectImProjectConfigByProjectIdAndSystemCode(projectId, systemCode);
    }

    /**
     * 查询项目通知配置列表
     * 
     * @param imProjectConfig 项目通知配置
     * @return 项目通知配置
     */
    @Override
    public List<IMProjectConfig> selectImProjectConfigList(IMProjectConfig imProjectConfig)
    {
        return imProjectConfigMapper.selectImProjectConfigList(imProjectConfig);
    }

    /**
     * 新增项目通知配置
     * 
     * @param imProjectConfig 项目通知配置
     * @return 结果
     */
    @Override
    public int insertImProjectConfig(IMProjectConfig imProjectConfig)
    {
        return imProjectConfigMapper.insertImProjectConfig(imProjectConfig);
    }

    /**
     * 修改项目通知配置
     * 
     * @param imProjectConfig 项目通知配置
     * @return 结果
     */
    @Override
    public int updateImProjectConfig(IMProjectConfig imProjectConfig)
    {
        return imProjectConfigMapper.updateImProjectConfig(imProjectConfig);
    }

    /**
     * 批量删除项目通知配置
     * 
     * @param configIds 需要删除的项目通知配置主键
     * @return 结果
     */
    @Override
    public int deleteImProjectConfigByConfigIds(Long[] configIds)
    {
        return imProjectConfigMapper.deleteImProjectConfigByConfigIds(configIds);
    }

    /**
     * 删除项目通知配置信息
     * 
     * @param configId 项目通知配置主键
     * @return 结果
     */
    @Override
    public int deleteImProjectConfigByConfigId(Long configId)
    {
        return imProjectConfigMapper.deleteImProjectConfigByConfigId(configId);
    }
}
