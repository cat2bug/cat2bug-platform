package com.cat2bug.common.utils;

import com.cat2bug.common.constant.FlywayConstants;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 判定目标库是否已完成 Flyway 迁移并具备业务表。
 * <p>
 * 仅检查 {@link FlywayConstants#SCHEMA_HISTORY_TABLE} 不足以代表 schema 就绪（例如 baseline 已写入但
 * 后续 SQL 未执行）；安装向导以 {@link #hasCoreSchema} 为准。
 */
public final class FlywaySchemaSupport
{
    private FlywaySchemaSupport()
    {
    }

    /** 业务核心表是否已存在（Flyway 迁移真正落地后的标志）。 */
    public static boolean hasCoreSchema(JdbcTemplate jdbcTemplate)
    {
        try
        {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class);
            return count != null;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static boolean hasCoreSchema(Connection connection)
    {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM sys_user");
                ResultSet rs = ps.executeQuery())
        {
            return rs.next();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static boolean hasSuccessfulMigration(JdbcTemplate jdbcTemplate)
    {
        try
        {
            Integer count = jdbcTemplate.queryForObject(FlywayConstants.COUNT_SUCCESSFUL_MIGRATIONS_SQL, Integer.class);
            return count != null && count > 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static boolean hasSuccessfulMigration(Connection connection)
    {
        try (PreparedStatement ps = connection.prepareStatement(FlywayConstants.COUNT_SUCCESSFUL_MIGRATIONS_SQL);
                ResultSet rs = ps.executeQuery())
        {
            return rs.next() && rs.getInt(1) > 0;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
