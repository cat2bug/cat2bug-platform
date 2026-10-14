package com.cat2bug.system.service;

import com.cat2bug.common.domain.im.IMProjectConfig;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mapper.IMProjectConfigMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ImProjectConfigService {

    private static final Logger LOG = Logger.getLogger(ImProjectConfigService.class);
    private static final ObjectMapper APP_CONFIG_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Inject
    IMProjectConfigMapper imProjectConfigMapper;

    public IMProjectConfig selectImProjectConfigByConfigId(Long configId) {
        return imProjectConfigMapper.selectImProjectConfigByConfigId(configId);
    }

    public IMProjectConfig selectImProjectConfigByProjectIdAndSystemCode(Long projectId, String systemCode) {
        return imProjectConfigMapper.selectImProjectConfigByProjectIdAndSystemCode(projectId, systemCode);
    }

    /**
     * 读取项目的企业应用配置（钉钉 / 飞书 / 企业微信）；未配置或 JSON 无法解析时返回 {@code null}。
     */
    public <T> T selectAppConfig(Long projectId, String systemCode, Class<T> type) {
        IMProjectConfig config = selectImProjectConfigByProjectIdAndSystemCode(projectId, systemCode);
        if (config == null || StringUtils.isBlank(config.getConfigParams())) {
            return null;
        }
        try {
            return APP_CONFIG_MAPPER.readValue(config.getConfigParams(), type);
        } catch (Exception e) {
            LOG.warnf("项目 IM 应用配置无法解析 projectId=%d systemCode=%s", projectId, systemCode);
            return null;
        }
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
