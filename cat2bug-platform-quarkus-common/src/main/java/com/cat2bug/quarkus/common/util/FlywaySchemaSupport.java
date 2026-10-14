package com.cat2bug.quarkus.common.util;

import com.cat2bug.quarkus.common.constant.FlywayConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 以 Flyway 版本表判定库表是否已由迁移初始化。
 */
public final class FlywaySchemaSupport {

    private FlywaySchemaSupport() {
    }

    public static boolean hasSuccessfulMigration(Connection connection) {
        try (PreparedStatement ps = connection.prepareStatement(FlywayConstants.COUNT_SUCCESSFUL_MIGRATIONS_SQL);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
