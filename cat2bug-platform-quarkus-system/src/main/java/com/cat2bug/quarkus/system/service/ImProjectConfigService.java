package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.domain.im.IMProjectConfig;
import com.cat2bug.quarkus.system.mapper.IMProjectConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ImProjectConfigService {

    @Inject
    IMProjectConfigMapper imProjectConfigMapper;

    public IMProjectConfig selectImProjectConfigByConfigId(Long configId) {
        return imProjectConfigMapper.selectImProjectConfigByConfigId(configId);
    }

    public IMProjectConfig selectImProjectConfigByProjectIdAndSystemCode(Long projectId, String systemCode) {
        return imProjectConfigMapper.selectImProjectConfigByProjectIdAndSystemCode(projectId, systemCode);
    }

    public List<IMProjectConfig> selectImProjectConfigList(IMProjectConfig imProjectConfig) {
        return imProjectConfigMapper.selectImProjectConfigList(imProjectConfig);
    }

    public int insertImProjectConfig(IMProjectConfig imProjectConfig) {
        return imProjectConfigMapper.insertImProjectConfig(imProjectConfig);
    }

    public int updateImProjectConfig(IMProjectConfig imProjectConfig) {
        return imProjectConfigMapper.updateImProjectConfig(imProjectConfig);
    }

    public int deleteImProjectConfigByConfigId(Long configId) {
        return imProjectConfigMapper.deleteImProjectConfigByConfigId(configId);
    }

    public int saveImProjectConfig(IMProjectConfig imProjectConfig, String systemCode) {
        IMProjectConfig existing = imProjectConfigMapper.selectImProjectConfigByProjectIdAndSystemCode(
                imProjectConfig.getProjectId(), systemCode);
        imProjectConfig.setSystemCode(systemCode);
        if (existing == null) {
            return insertImProjectConfig(imProjectConfig);
        }
        imProjectConfig.setConfigId(existing.getConfigId());
        return updateImProjectConfig(imProjectConfig);
    }
}
