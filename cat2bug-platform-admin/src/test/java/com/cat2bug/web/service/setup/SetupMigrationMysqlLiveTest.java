package com.cat2bug.web.service.setup;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 针对本机 MySQL 的 live 验证：legacy 库已存在 sys_robot_agent 且 V0_6_2 迁移失败时可重试成功。
 */
class SetupMigrationMysqlLiveTest
{
    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/cat2bug_platform"
            + "?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull"
            + "&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8";

    @Test
    void migrate_retriesAfterFailedV062WhenTablesAlreadyExist() throws Exception
    {
        Assumptions.assumeTrue(canConnectMysql(), "local MySQL cat2bug_platform not available");

        try (DruidDataSource dataSource = createDataSource())
        {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Integer robotTable = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_robot_agent'",
                    Integer.class);
            Assumptions.assumeTrue(robotTable != null && robotTable > 0,
                    "sys_robot_agent not present — skip live repair test");

            SetupMigrationService migrationService = new SetupMigrationService();
            migrationService.migrate("mysql", dataSource);

            Integer failed = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_db_version WHERE version = '0.6.2' AND success = 0",
                    Integer.class);
            assertEquals(0, failed == null ? 0 : failed);

            Integer applied = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_db_version WHERE version = '0.6.2' AND success = 1",
                    Integer.class);
            assertTrue(applied != null && applied >= 1);
        }
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
