package com.cat2bug.admin.service;

import com.cat2bug.system.service.oauth.OAuthService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * 清理过期授权码、长期失效的令牌，以及 30 天未使用且无有效令牌的 OAuth 客户端。
 */
@ApplicationScoped
public class OAuthCleanupJob {

    private static final Logger LOG = Logger.getLogger(OAuthCleanupJob.class);

    @Inject
    OAuthService oauthService;

    @Scheduled(every = "6h", delayed = "10m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void cleanup() {
        try {
            oauthService.cleanup();
        } catch (RuntimeException e) {
            // 未完成安装时数据源不可用，下次再试
            LOG.debugf("OAuth cleanup skipped: %s", e.getMessage());
        }
    }
}
