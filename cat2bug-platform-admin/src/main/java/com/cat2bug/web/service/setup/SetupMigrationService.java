package com.cat2bug.web.service.setup;

import com.cat2bug.common.constant.FlywayConstants;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.service.UpgradeMigrationInspector;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 安装/升级阶段执行 Flyway 迁移。
 */
@Service
public class SetupMigrationService implements UpgradeMigrationInspector
{
    private static final Logger log = LoggerFactory.getLogger(SetupMigrationService.class);

    /** 避免 UpgradeFilter 等热路径每次请求都初始化 Flyway。 */
    private static final long PENDING_CACHE_TTL_MS = 300_000L;

    /** 连接池争用时短暂复用空结果，避免惊群重试。 */
    private static final long PENDING_FAILURE_CACHE_TTL_MS = 30_000L;

    @Autowired
    private DataSource dataSource;

    private volatile CachedPendingMigrations pendingCache;

    private volatile Thread pendingLoadOwner;

    private final Object pendingCacheLock = new Object();

    public void migrate(String databaseType, DataSource dataSourceOverride)
    {
        String type = StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2";
        DataSource target = dataSourceOverride != null ? dataSourceOverride : dataSource;
        try
        {
            executeMigrate(type, target);
        }
        catch (FlywayException e)
        {
            if (isRepairableMigrationError(e))
            {
                log.warn("Flyway 迁移异常，自动 repair 后重试 migrate（{}）: {}", type, e.getMessage());
                repair(type, target);
                try
                {
                    executeMigrate(type, target);
                    return;
                }
                catch (FlywayException retryEx)
                {
                    throw new ServiceException(SetupMessages.msg("setup.install.migration.failed", retryEx.getMessage()));
                }
            }
            throw new ServiceException(SetupMessages.msg("setup.install.migration.failed", e.getMessage()));
        }
    }

    private void executeMigrate(String databaseType, DataSource target)
    {
        Flyway flyway = buildFlyway(databaseType, target);
        repairFailedMigrationsIfNeeded(flyway);
        flyway.migrate();
        invalidatePendingMigrationCache();
    }

    private static void repairFailedMigrationsIfNeeded(Flyway flyway)
    {
        for (MigrationInfo migration : flyway.info().all())
        {
            if (MigrationState.FAILED.equals(migration.getState()))
            {
                log.warn("检测到失败的 Flyway 迁移 {}，执行 repair", migration.getVersion());
                flyway.repair();
                return;
            }
        }
    }

    static boolean isRepairableMigrationError(Throwable error)
    {
        Throwable current = error;
        while (current != null)
        {
            if (containsChecksumConflict(current.getMessage()) || containsFailedMigration(current.getMessage()))
            {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public void repairIfChecksumConflict(String databaseType, DataSource dataSourceOverride, String errorMessage)
    {
        if (!containsChecksumConflict(errorMessage))
        {
            return;
        }
        String type = StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2";
        repair(type, dataSourceOverride != null ? dataSourceOverride : dataSource);
    }

    private void repair(String databaseType, DataSource target)
    {
        buildFlyway(databaseType, target).repair();
        invalidatePendingMigrationCache();
    }

    static boolean isChecksumConflict(Throwable error)
    {
        return isRepairableMigrationError(error) && containsChecksumConflictInChain(error);
    }

    private static boolean containsChecksumConflictInChain(Throwable error)
    {
        Throwable current = error;
        while (current != null)
        {
            if (containsChecksumConflict(current.getMessage()))
            {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    static boolean containsFailedMigration(String message)
    {
        if (message == null || message.isBlank())
        {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("failed migration") || lower.contains("migrations have failed");
    }

    static boolean containsChecksumConflict(String message)
    {
        if (message == null || message.isBlank())
        {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("checksum mismatch") || lower.contains("migration checksum");
    }

    @Override
    public List<String> listPendingMigrations(String databaseType)
    {
        String type = StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2";
        return List.copyOf(loadPendingMigrationsCached(type));
    }

    /** 安装/升级完成后或配置变更时调用，使待执行迁移检测重新读取 Flyway。 */
    public void invalidatePendingMigrationCache()
    {
        synchronized (pendingCacheLock)
        {
            pendingCache = null;
            pendingLoadOwner = null;
            pendingCacheLock.notifyAll();
        }
    }

    private List<String> loadPendingMigrationsCached(String databaseType)
    {
        long now = System.currentTimeMillis();
        CachedPendingMigrations cached = pendingCache;
        if (isPendingCacheValid(cached, databaseType, now))
        {
            return cached.pending();
        }
        synchronized (pendingCacheLock)
        {
            cached = pendingCache;
            if (isPendingCacheValid(cached, databaseType, now))
            {
                return cached.pending();
            }
            while (pendingLoadOwner != null && pendingLoadOwner != Thread.currentThread())
            {
                try
                {
                    pendingCacheLock.wait(PENDING_FAILURE_CACHE_TTL_MS);
                }
                catch (InterruptedException interrupted)
                {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
                cached = pendingCache;
                if (isPendingCacheValid(cached, databaseType, System.currentTimeMillis()))
                {
                    return cached.pending();
                }
            }
            pendingLoadOwner = Thread.currentThread();
        }
        try
        {
            List<String> pending = fetchPendingFromFlyway(databaseType);
            synchronized (pendingCacheLock)
            {
                pendingCache = new CachedPendingMigrations(databaseType, List.copyOf(pending), System.currentTimeMillis(), false);
                return pending;
            }
        }
        finally
        {
            synchronized (pendingCacheLock)
            {
                pendingLoadOwner = null;
                pendingCacheLock.notifyAll();
            }
        }
    }

    private static boolean isPendingCacheValid(CachedPendingMigrations cached, String databaseType, long now)
    {
        if (cached == null || !databaseType.equals(cached.databaseType()))
        {
            return false;
        }
        long ttl = cached.failedProbe() ? PENDING_FAILURE_CACHE_TTL_MS : PENDING_CACHE_TTL_MS;
        return now - cached.loadedAtMs() < ttl;
    }

    private List<String> fetchPendingFromFlyway(String databaseType)
    {
        DataSource target = dataSource;
        try
        {
            return listPendingFromFlyway(databaseType, target);
        }
        catch (Exception e)
        {
            if (isRepairableMigrationError(e))
            {
                log.warn("Flyway pending 检测异常，自动 repair 后重试（{}）: {}", databaseType, e.getMessage());
                repair(databaseType, target);
                try
                {
                    return listPendingFromFlyway(databaseType, target);
                }
                catch (Exception retryEx)
                {
                    log.warn("Flyway pending 检测重试仍失败（{}）: {}", databaseType, retryEx.getMessage());
                    cacheFailedPendingProbe(databaseType);
                    return List.of();
                }
            }
            log.warn("Flyway pending 检测失败（{}）: {}", databaseType, e.getMessage());
            cacheFailedPendingProbe(databaseType);
            return List.of();
        }
    }

    private void cacheFailedPendingProbe(String databaseType)
    {
        synchronized (pendingCacheLock)
        {
            pendingCache = new CachedPendingMigrations(databaseType, List.of(), System.currentTimeMillis(), true);
        }
    }

    private record CachedPendingMigrations(String databaseType, List<String> pending, long loadedAtMs, boolean failedProbe)
    {
        CachedPendingMigrations(String databaseType, List<String> pending, long loadedAtMs)
        {
            this(databaseType, pending, loadedAtMs, false);
        }
    }

    private List<String> listPendingFromFlyway(String databaseType, DataSource target)
    {
        Flyway flyway = buildFlyway(databaseType, target);
        repairFailedMigrationsIfNeeded(flyway);
        MigrationInfoService info = flyway.info();
        List<String> pending = new ArrayList<>();
        for (MigrationInfo migration : info.pending())
        {
            pending.add(migration.getVersion() + " " + migration.getDescription());
        }
        return pending;
    }

    @Override
    public boolean hasPendingMigrations(String databaseType)
    {
        return !listPendingMigrations(databaseType).isEmpty();
    }

    private Flyway buildFlyway(String databaseType, DataSource dataSourceOverride)
    {
        var config = Flyway.configure()
                .locations("classpath:db/migration/" + databaseType)
                .table(FlywayConstants.SCHEMA_HISTORY_TABLE)
                .encoding("UTF-8")
                .baselineOnMigrate(true)
                .baselineVersion("0.5.0")
                .baselineDescription("BaseLineInitialize")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .dataSource(dataSourceOverride != null ? dataSourceOverride : dataSource);
        return config.load();
    }
}
