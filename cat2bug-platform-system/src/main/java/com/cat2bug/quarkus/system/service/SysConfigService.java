package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.api.TableDataInfo;
import com.cat2bug.quarkus.common.core.page.PageDomain;
import com.cat2bug.quarkus.common.domain.SysConfig;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.framework.cache.AppCache;
import com.cat2bug.quarkus.system.mapper.SysConfigMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class SysConfigService {

    private static final Logger LOG = Logger.getLogger(SysConfigService.class);
    private static final String REGION = "config";

    @Inject
    AppCache appCache;

    @Inject
    SysConfigMapper sysConfigMapper;

    public String selectConfigByKey(String configKey) {
        String cached = appCache.get(REGION, configKey, String.class);
        if (StringUtils.isNotEmpty(cached)) {
            return cached;
        }
        SysConfig config = sysConfigMapper.selectConfig(null, configKey);
        if (config != null && StringUtils.isNotEmpty(config.getConfigValue())) {
            appCache.set(REGION, configKey, config.getConfigValue());
            return config.getConfigValue();
        }
        return "";
    }

    public SysConfig selectConfigById(Long configId) {
        return sysConfigMapper.selectConfig(configId, null);
    }

    public TableDataInfo selectConfigPage(SysConfig query, Integer pageNum, Integer pageSize) {
        PageDomain page = PageDomain.of(pageNum, pageSize);
        long total = sysConfigMapper.countConfigList(query);
        List<SysConfig> all = sysConfigMapper.selectConfigList(query);
        int from = Math.min(page.getOffset(), all.size());
        int to = Math.min(from + page.getPageSize(), all.size());
        return new TableDataInfo(all.subList(from, to), total);
    }

    public List<SysConfig> selectConfigList(SysConfig query) {
        List<SysConfig> list = sysConfigMapper.selectConfigList(query);
        return list != null ? list : List.of();
    }

    public void loadConfigCache() {
        appCache.clearRegion(REGION);
        List<SysConfig> list = sysConfigMapper.selectConfigList(new SysConfig());
        if (list == null) {
            return;
        }
        for (SysConfig config : list) {
            if (config.getConfigKey() != null && config.getConfigValue() != null) {
                appCache.set(REGION, config.getConfigKey(), config.getConfigValue());
            }
        }
        LOG.infof("参数缓存已加载：%d 条", list.size());
    }

    public void clearConfigCache() {
        appCache.clearRegion(REGION);
    }

    public void resetConfigCache() {
        clearConfigCache();
        loadConfigCache();
    }

    public boolean checkConfigKeyUnique(SysConfig config) {
        Long configId = config.getConfigId() == null ? -1L : config.getConfigId();
        SysConfig info = sysConfigMapper.checkConfigKeyUnique(config.getConfigKey());
        return info == null || info.getConfigId().longValue() == configId.longValue();
    }

    public int insertConfig(SysConfig config) {
        int row = sysConfigMapper.insertConfig(config);
        if (row > 0) {
            setConfigCache(config.getConfigKey(), config.getConfigValue());
        }
        return row;
    }

    public int updateConfig(SysConfig config) {
        SysConfig existing = selectConfigById(config.getConfigId());
        if (existing != null && !StringUtils.equals(existing.getConfigKey(), config.getConfigKey())) {
            removeConfigCache(existing.getConfigKey());
        }
        int row = sysConfigMapper.updateConfig(config);
        if (row > 0) {
            setConfigCache(config.getConfigKey(), config.getConfigValue());
        }
        return row;
    }

    public void deleteConfigByIds(Long[] configIds) {
        for (Long configId : configIds) {
            SysConfig config = selectConfigById(configId);
            if (config == null) {
                continue;
            }
            if ("Y".equals(config.getConfigType())) {
                throw new ApiException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            sysConfigMapper.deleteConfigById(configId);
            removeConfigCache(config.getConfigKey());
        }
    }

    private void setConfigCache(String configKey, String configValue) {
        if (configKey != null && configValue != null) {
            appCache.set(REGION, configKey, configValue);
        }
    }

    private void removeConfigCache(String configKey) {
        if (configKey != null) {
            appCache.remove(REGION, configKey);
        }
    }

    public boolean selectCaptchaEnabled() {
        String captchaEnabled = selectConfigByKey("sys.account.captchaEnabled");
        if (StringUtils.isEmpty(captchaEnabled)) {
            return false;
        }
        return "true".equalsIgnoreCase(captchaEnabled.trim());
    }

    public boolean selectRegisterEnabled() {
        String registerUser = selectConfigByKey("sys.account.registerUser");
        if (StringUtils.isEmpty(registerUser)) {
            return true;
        }
        return "true".equalsIgnoreCase(registerUser.trim());
    }
}
