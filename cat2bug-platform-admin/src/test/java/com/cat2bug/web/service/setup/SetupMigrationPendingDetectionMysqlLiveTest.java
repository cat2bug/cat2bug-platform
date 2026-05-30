package com.cat2bug.web.service.setup;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证手工删除 sys_db_version 后仍能检测到待执行迁移。
 */
class SetupMigrationPendingDetectionMysqlLiveTest
{
    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/cat2bug_platform"
            + "?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull"
            + "&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8";

    @Test
    void listPendingMigrations_detectsDriftWhenVersionTableBehind()
    {
        Assumptions.assumeTrue(canConnectMysql(), "local MySQL cat2bug_platform not available");

        try (DruidDataSource dataSource = createDataSource())
        {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Integer versionCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_db_version", Integer.class);
            Assumptions.assumeTrue(versionCount != null && versionCount >= 2,
                    "sys_db_version has too few rows — skip live drift test");

            List<Map<String, Object>> backup = jdbc.queryForList(
                    "SELECT installed_rank, version, description, type, script, checksum, "
                            + "installed_by, installed_on, execution_time, success "
                            + "FROM sys_db_version ORDER BY installed_rank");

            try
            {
                jdbc.update("DELETE FROM sys_db_version WHERE version NOT IN ('0.5.1', '0.6.0')");

                SetupMigrationService migrationService = new SetupMigrationService();
                ReflectionTestUtils.setField(migrationService, "dataSource", dataSource);

                List<String> pending = migrationService.listPendingMigrations("mysql");
                assertFalse(pending.isEmpty(),
                        "expected pending migrations when sys_db_version is behind schema scripts");
            }
            finally
            {
                restoreVersionTable(jdbc, backup);
            }
        }
    }

    private static void restoreVersionTable(JdbcTemplate jdbc, List<Map<String, Object>> backup)
    {
        jdbc.update("DELETE FROM sys_db_version");
        for (Map<String, Object> row : backup)
        {
            jdbc.update(
                    "INSERT INTO sys_db_version "
                            + "(installed_rank, version, description, type, script, checksum, "
                            + "installed_by, installed_on, execution_time, success) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    row.get("installed_rank"),
                    row.get("version"),
                    row.get("description"),
                    row.get("type"),
                    row.get("script"),
                    row.get("checksum"),
                    row.get("installed_by"),
                    toTimestamp(row.get("installed_on")),
                    row.get("execution_time"),
                    row.get("success"));
        }
    }

    private static Timestamp toTimestamp(Object value)
    {
        if (value instanceof Timestamp timestamp)
        {
            return timestamp;
        }
        if (value instanceof java.util.Date date)
        {
            return new Timestamp(date.getTime());
        }
        return null;
    }

    private static boolean canConnectMysql()
    {
        try (DruidDataSource dataSource = createDataSource();
                Connection ignored = dataSource.getConnection())
        {
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static DruidDataSource createDataSource()
    {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(JDBC_URL);
        dataSource.setUsername("root");
        dataSource.setPassword("cat2bug_password");
        dataSource.setInitialSize(1);
        dataSource.setMaxActive(2);
        return dataSource;
    }
}
