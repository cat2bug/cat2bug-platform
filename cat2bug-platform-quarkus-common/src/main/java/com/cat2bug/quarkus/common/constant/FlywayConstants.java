package com.cat2bug.quarkus.common.constant;

/**
 * Flyway 元数据表（与 SetupMigrationService 中 table 配置一致）。
 */
public final class FlywayConstants {

    private FlywayConstants() {
    }

    public static final String SCHEMA_HISTORY_TABLE = "sys_db_version";

    public static final String COUNT_SUCCESSFUL_MIGRATIONS_SQL =
            "SELECT COUNT(*) FROM " + SCHEMA_HISTORY_TABLE + " WHERE success = 1";

    public static final String LEGACY_LINE_MAX_VERSION = "0.6.2.18";

    public static final String PRODUCT_VERSION = "1.0.0";
}
