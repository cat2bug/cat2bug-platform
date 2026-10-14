package com.cat2bug.admin.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 启动后在后台确保默认业务模型已下载，对齐 Spring {@code OllamaAiServieImpl#init}；不阻塞启动、失败不重试。
 */
@ApplicationScoped
public class OllamaDefaultModelBootstrap {

    private static final Logger LOG = Logger.getLogger(OllamaDefaultModelBootstrap.class);
    private static final String LATEST_TAG = ":latest";

    @Inject
    OllamaAvailabilityService ollamaAvailability;

    @Inject
    OllamaModelService ollamaModelService;

    void onStart(@Observes StartupEvent event) {
        if (!ollamaAvailability.isAvailable()) {
            return;
        }
        List<String> defaults = ollamaAvailability.defaultModelNames();
        if (defaults.isEmpty()) {
            return;
        }
        Thread thread = new Thread(() -> ensureDefaultModels(defaults), "ollama-default-model");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * @return 本次发起拉取的模型名
     */
    List<String> ensureDefaultModels(List<String> modelNames) {
        Set<String> downloaded;
        try {
            downloaded = ollamaAvailability.fetchDownloadedModels().stream()
                    .map(row -> String.valueOf(row.get("name")))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LOG.warnf("无法连接 Ollama（%s），跳过默认模型检查: %s",
                    ollamaAvailability.normalizeHost(), e.getMessage());
            return List.of();
        }
        List<String> pulled = new ArrayList<>();
        for (String name : modelNames) {
            if (isDownloaded(downloaded, name)) {
                continue;
            }
            LOG.infof("开始下载模型: %s", name);
            try {
                ollamaModelService.pullModule(name, true);
                pulled.add(name);
            } catch (Exception e) {
                LOG.warnf("默认模型 %s 拉取失败: %s", name, e.getMessage());
            }
        }
        return pulled;
    }

    private static boolean isDownloaded(Set<String> downloaded, String name) {
        return downloaded.contains(name) || (!name.contains(":") && downloaded.contains(name + LATEST_TAG));
    }
}
