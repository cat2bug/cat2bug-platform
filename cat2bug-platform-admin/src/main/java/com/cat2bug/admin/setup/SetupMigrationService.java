package com.cat2bug.admin.setup;

import com.cat2bug.common.constant.FlywayConstants;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.setup.SetupMessages;
import com.cat2bug.common.util.StringUtils;
import io.quarkus.flyway.runtime.QuarkusFlywayClassProvider;
import io.quarkus.flyway.runtime.QuarkusFlywayResourceProvider;
import io.quarkus.flyway.runtime.QuarkusPathLocationScanner;
import jakarta.enterprise.context.ApplicationScoped;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.graalvm.nativeimage.ImageInfo;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class SetupMigrationService {

    private static final Logger log = Logger.getLogger(SetupMigrationService.class);
    private static final long PENDING_CACHE_TTL_MS = 30_000L;
    private static final long PENDING_FAILURE_CACHE_TTL_MS = 5_000L;

    private final Object pendingCacheLock = new Object();
    private volatile CachedPendingMigrations pendingCache;
    private volatile Thread pendingLoadOwner;

    @jakarta.inject.Inject
    InstallEffectiveDataSource effectiveDataSource;

    public void migrate(String databaseType, DataSource dataSourceOverride) {
        try {
            executeMigrate(dataSourceOverride);
        } catch (FlywayException e) {
            if (isRepairableMigrationError(e)) {
                log.warnf("Flyway 迁移异常，自动 repair 后重试: %s", e.getMessage());
                repair(dataSourceOverride);
                try {
                    executeMigrate(dataSourceOverride);
                    return;
                } catch (FlywayException retryEx) {
                    throw new ApiException(SetupMessages.msg("setup.install.migration.failed", retryEx.getMessage()));
                }
            }
            throw new ApiException(SetupMessages.msg("setup.install.migration.failed", e.getMessage()));
        }
    }

    private void executeMigrate(DataSource target) {
        Flyway flyway = buildFlyway(target);
        repairFailedMigrationsIfNeeded(flyway);
        // 已有业务表但无成功迁移记录时，baselineOnMigrate 不会生效（历史表已存在），
        // 必须显式 baseline，否则会重跑破坏性 V0_5_1__init.sql（DROP 全库）。
        // Flyway 要求：空的 schema history 表须先删除才能 baseline。
        if (hasBusinessSchema(target) && !hasSuccessfulMigration(flyway)) {
            dropEmptySchemaHistoryTable(target);
            flyway = buildFlyway(target);
            log.warn("检测到已有业务库且无 Flyway 成功记录，按 0.5.1 baseline 跳过 init");
            flyway.baseline();
        }
        flyway.migrate();
    }

    private void dropEmptySchemaHistoryTable(DataSource dataSource) {
        String table = FlywayConstants.SCHEMA_HISTORY_TABLE;
        try (Connection connection = dataSource.getConnection();
             var countPs = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             var countRs = countPs.executeQuery()) {
            long rows = 0;
            if (countRs.next()) {
                rows = countRs.getLong(1);
            }
            if (rows > 0) {
                return;
            }
            try (var drop = connection.createStatement()) {
                drop.execute("DROP TABLE " + table);
                log.warnf("已删除空的 Flyway 历史表 %s，以便 baseline", table);
            }
        } catch (SQLException e) {
            // 表不存在则无需处理
            log.debugf("删除空 Flyway 历史表跳过: %s", e.getMessage());
        }
    }

    /** 判断是否已有业务库（相对空库安装）。 */
    static boolean hasBusinessSchema(DataSource dataSource) {
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection();
             var ps = connection.prepareStatement(
                     "SELECT 1 FROM information_schema.tables WHERE LOWER(table_name) = 'sys_user' LIMIT 1");
             var rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            log.warnf("检测业务表失败，按空库处理: %s", e.getMessage());
            return false;
        }
    }

    private static boolean hasSuccessfulMigration(Flyway flyway) {
        for (MigrationInfo migration : flyway.info().all()) {
            MigrationState state = migration.getState();
            if (MigrationState.SUCCESS.equals(state) || MigrationState.BASELINE.equals(state)) {
                return true;
            }
        }
        return false;
    }

    private static void repairFailedMigrationsIfNeeded(Flyway flyway) {
        for (MigrationInfo migration : flyway.info().all()) {
            if (MigrationState.FAILED.equals(migration.getState())) {
                log.warnf("检测到失败的 Flyway 迁移 %s，执行 repair", migration.getVersion());
                flyway.repair();
                return;
            }
        }
    }

    static boolean isRepairableMigrationError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (containsChecksumConflict(current.getMessage()) || containsFailedMigration(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public void repairIfChecksumConflict(String databaseType, DataSource dataSourceOverride, String errorMessage) {
        if (!containsChecksumConflict(errorMessage)) {
            return;
        }
        repair(dataSourceOverride != null ? dataSourceOverride : effectiveDataSource.get());
    }

    public void invalidatePendingMigrationCache() {
        synchronized (pendingCacheLock) {
            pendingCache = null;
            pendingLoadOwner = null;
            pendingCacheLock.notifyAll();
        }
    }

    public List<String> listPendingMigrations(String databaseType) {
        return List.copyOf(loadPendingMigrationsCached(
                StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2"));
    }

    private void repair(DataSource target) {
        buildFlyway(target).repair();
        invalidatePendingMigrationCache();
    }

    private List<String> loadPendingMigrationsCached(String databaseType) {
        long now = System.currentTimeMillis();
        CachedPendingMigrations cached = pendingCache;
        if (isPendingCacheValid(cached, databaseType, now)) {
            return cached.pending();
        }
        synchronized (pendingCacheLock) {
            cached = pendingCache;
            if (isPendingCacheValid(cached, databaseType, now)) {
                return cached.pending();
            }
            while (pendingLoadOwner != null && pendingLoadOwner != Thread.currentThread()) {
                try {
                    pendingCacheLock.wait(PENDING_FAILURE_CACHE_TTL_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
                cached = pendingCache;
                if (isPendingCacheValid(cached, databaseType, System.currentTimeMillis())) {
                    return cached.pending();
                }
            }
            pendingLoadOwner = Thread.currentThread();
        }
        try {
            List<String> pending = fetchPendingFromFlyway();
            synchronized (pendingCacheLock) {
                pendingCache = new CachedPendingMigrations(databaseType, List.copyOf(pending), System.currentTimeMillis(), false);
                return pending;
            }
        } finally {
            synchronized (pendingCacheLock) {
                pendingLoadOwner = null;
                pendingCacheLock.notifyAll();
            }
        }
    }

    private static boolean isPendingCacheValid(CachedPendingMigrations cached, String databaseType, long now) {
        if (cached == null || !databaseType.equals(cached.databaseType())) {
            return false;
        }
        long ttl = cached.failedProbe() ? PENDING_FAILURE_CACHE_TTL_MS : PENDING_CACHE_TTL_MS;
        return now - cached.loadedAtMs() < ttl;
    }

    private List<String> fetchPendingFromFlyway() {
        try {
            return listPendingFromFlyway(effectiveDataSource.get());
        } catch (Exception e) {
            if (isRepairableMigrationError(e)) {
                log.warnf("Flyway pending 检测异常，自动 repair 后重试: %s", e.getMessage());
                repair(effectiveDataSource.get());
                try {
                    return listPendingFromFlyway(effectiveDataSource.get());
                } catch (Exception retryEx) {
                    log.warnf("Flyway pending 检测重试仍失败: %s", retryEx.getMessage());
                    cacheFailedPendingProbe("h2");
                    return List.of();
                }
            }
            log.warnf("Flyway pending 检测失败: %s", e.getMessage());
            cacheFailedPendingProbe("h2");
            return List.of();
        }
    }

    private void cacheFailedPendingProbe(String databaseType) {
        synchronized (pendingCacheLock) {
            pendingCache = new CachedPendingMigrations(databaseType, List.of(), System.currentTimeMillis(), true);
        }
    }

    private List<String> listPendingFromFlyway(DataSource target) {
        Flyway flyway = buildFlyway(target);
        repairFailedMigrationsIfNeeded(flyway);
        MigrationInfoService info = flyway.info();
        List<String> pending = new ArrayList<>();
        for (MigrationInfo migration : info.pending()) {
            pending.add(migration.getVersion() + " " + migration.getDescription());
        }
        return pending;
    }

    private record CachedPendingMigrations(String databaseType, List<String> pending, long loadedAtMs, boolean failedProbe) {
    }

    static boolean containsFailedMigration(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("failed migration") || lower.contains("migrations have failed");
    }

    static boolean containsChecksumConflict(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("checksum mismatch") || lower.contains("migration checksum");
    }

    static final String H2_LOCATION = "classpath:db/migration";
    static final String MYSQL_LOCATION = "classpath:db/mysql";

    /** H2 与 MySQL 脚本语法不同（与 Spring db/migration/{h2,mysql} 分别一致），按目标库实际产品选目录。 */
    static String migrationLocation(DataSource target) {
        if (target == null) {
            return H2_LOCATION;
        }
        try (Connection connection = target.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase(Locale.ROOT).contains("mysql") ? MYSQL_LOCATION : H2_LOCATION;
        } catch (SQLException e) {
            log.warnf("识别数据库类型失败，按 H2 脚本迁移: %s", e.getMessage());
            return H2_LOCATION;
        }
    }

    private Flyway buildFlyway(DataSource dataSourceOverride) {
        // 空库：0.5.0 → 再跑 V0_5_1 init；已有业务库：0.5.1，跳过整库 DROP/CREATE
        String baselineVersion = hasBusinessSchema(dataSourceOverride) ? "0.5.1" : "0.5.0";
        FluentConfiguration configuration = Flyway.configure()
                .locations(migrationLocation(dataSourceOverride))
                .table(FlywayConstants.SCHEMA_HISTORY_TABLE)
                .encoding("UTF-8")
                .baselineOnMigrate(true)
                .baselineVersion(baselineVersion)
                .baselineDescription("BaseLineInitialize")
                .validateOnMigrate(true)
                // 与 Spring 共用 sys_db_version，双方补入的低版本脚本须能在已升级库上补跑
                .outOfOrder(true)
                .dataSource(dataSourceOverride);
        if (ImageInfo.inImageRuntimeCode()) {
            // Native 下 Flyway 的 classpath Scanner 被 Quarkus 替换为不可用；
            // 与 Quarkus FlywayCreator 一致，改用构建期索引的迁移文件（quarkus.flyway.*locations）。
            QuarkusPathLocationScanner scanner = new QuarkusPathLocationScanner(
                    configuration, Arrays.asList(configuration.getLocations()));
            configuration.resourceProvider(new QuarkusFlywayResourceProvider(scanner.scanForResources()));
            configuration.javaMigrationClassProvider(new QuarkusFlywayClassProvider(scanner.scanForClasses()));
        }
        return configuration.load();
    }
}
