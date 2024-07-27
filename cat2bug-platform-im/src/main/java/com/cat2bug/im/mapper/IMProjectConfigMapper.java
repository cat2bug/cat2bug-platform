package com.cat2bug.im.mapper;

import java.util.List;
import com.cat2bug.im.domain.IMProjectConfig;
import org.apache.ibatis.annotations.Param;

/**
 * 项目通知配置Mapper接口
 * 
 * @author yuzhantao
 * @date 2024-07-27
 */
public interface IMProjectConfigMapper
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
     * @param projectId 项目ID
     * @param systemCode 平台编码
     * @return 项目通知配置
     */
    public IMProjectConfig selectImProjectConfigByProjectIdAndSystemCode(@Param("projectId")Long projectId,@Param("systemCode") String systemCode);

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
     * 删除项目通知配置
     * 
     * @param configId 项目通知配置主键
     * @return 结果
     */
    public int deleteImProjectConfigByConfigId(Long configId);

    /**
     * 批量删除项目通知配置
     * 
     * @param configIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteImProjectConfigByConfigIds(Long[] configIds);
}
