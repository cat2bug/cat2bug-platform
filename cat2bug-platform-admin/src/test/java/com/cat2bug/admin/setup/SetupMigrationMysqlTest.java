package com.cat2bug.admin.setup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 在真实 MySQL 上从空库全量迁移。需设置 {@code CAT2BUG_TEST_MYSQL_URL}（不含库名，如
 * {@code jdbc:mysql://127.0.0.1:3306/}）、{@code CAT2BUG_TEST_MYSQL_USER}、{@code CAT2BUG_TEST_MYSQL_PASSWORD}；
 * 测试会创建并删除临时库。
 */
@EnabledIfEnvironmentVariable(named = "CAT2BUG_TEST_MYSQL_URL", matches = ".+")
class SetupMigrationMysqlTest {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String PARAMS = "?useUnicode=true&characterEncoding=utf8&useSSL=false"
            + "&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8";

    @Test
    void freshMysqlMigratesWithMysqlScripts() throws Exception {
        String base = System.getenv("CAT2BUG_TEST_MYSQL_URL");
        String user = System.getenv("CAT2BUG_TEST_MYSQL_USER");
        String password = System.getenv("CAT2BUG_TEST_MYSQL_PASSWORD");
        String db = "cat2bug_qk_mig_" + System.currentTimeMillis();
        SimpleDataSource admin = new SimpleDataSource(DRIVER, base + PARAMS, user, password);
        try (Connection c = admin.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + db + " DEFAULT CHARACTER SET utf8mb4");
        }
        try {
            SimpleDataSource target = new SimpleDataSource(DRIVER, base + db + PARAMS, user, password);
            assertEquals(SetupMigrationService.MYSQL_LOCATION, SetupMigrationService.migrationLocation(target));
            new SetupMigrationService().migrate("mysql", target);
            try (Connection c = target.getConnection(); Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("select count(*), sum(case when success = 1 then 0 else 1 end) from sys_db_version")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) > 30, "应执行全部迁移脚本");
                assertEquals(0, rs.getInt(2), "不应有失败的迁移");
            }
        } finally {
            try (Connection c = admin.getConnection(); Statement s = c.createStatement()) {
                s.execute("DROP DATABASE IF EXISTS " + db);
            }
        }
    }
}
