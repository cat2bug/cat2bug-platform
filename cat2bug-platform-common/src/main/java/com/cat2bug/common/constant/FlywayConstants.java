package com.cat2bug.common.constant;

/**
 * Flyway 元数据表（与 application.yml / SetupMigrationService 中 table 配置一致）。
 */
public final class FlywayConstants
{
    private FlywayConstants()
    {
    }

    public static final String SCHEMA_HISTORY_TABLE = "sys_db_version";

    public static final String COUNT_SUCCESSFUL_MIGRATIONS_SQL =
            "SELECT COUNT(*) FROM " + SCHEMA_HISTORY_TABLE + " WHERE success = 1";
}
