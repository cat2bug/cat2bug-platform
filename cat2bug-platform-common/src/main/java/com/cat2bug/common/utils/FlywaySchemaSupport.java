package com.cat2bug.common.utils;

import com.cat2bug.common.constant.FlywayConstants;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 以 Flyway 版本表 {@link FlywayConstants#SCHEMA_HISTORY_TABLE} 判定库表是否已由迁移初始化。
 */
public final class FlywaySchemaSupport
{
    private FlywaySchemaSupport()
    {
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
