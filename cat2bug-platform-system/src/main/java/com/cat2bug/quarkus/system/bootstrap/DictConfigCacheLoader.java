package com.cat2bug.quarkus.system.bootstrap;

import com.cat2bug.quarkus.system.service.SysConfigService;
import com.cat2bug.quarkus.system.service.SysDictDataService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DictConfigCacheLoader {

    private static final Logger LOG = Logger.getLogger(DictConfigCacheLoader.class);

    @Inject
    SysDictDataService sysDictDataService;

    @Inject
    SysConfigService sysConfigService;

    void onStart(@Observes StartupEvent event) {
        try {
            sysDictDataService.loadDictCache();
            sysConfigService.loadConfigCache();
        } catch (Exception e) {
            LOG.warnf("字典/参数缓存未加载（数据库可能尚未就绪）: %s", e.getMessage());
        }
    }
}
