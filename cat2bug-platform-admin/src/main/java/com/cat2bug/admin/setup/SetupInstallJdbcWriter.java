package com.cat2bug.admin.setup;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.util.FlywaySchemaSupport;
import com.cat2bug.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@ApplicationScoped
public class SetupInstallJdbcWriter {

    public static final String INSTALL_COMPLETED_KEY = "cat2bug.install.completed";

    public void createOrUpdateAdmin(DataSource dataSource, SetupSubmitRequest request) {
        String username = request.getAdminUsername().trim();
        String encrypted = BCrypt.withDefaults().hashToString(12, request.getAdminPassword().toCharArray());
        String nickName = StringUtils.isNotEmpty(request.getAdminNickName()) ? request.getAdminNickName() : username;

        executeUpdate(dataSource,
                "DELETE FROM sys_user WHERE user_id = 1 OR user_name = ?",
                username);
        executeUpdate(dataSource, "DELETE FROM sys_user_role WHERE user_id = 1");
        executeUpdate(dataSource,
                "INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, password, status, del_flag, create_by, create_time) "
                        + "VALUES (1, 0, ?, ?, ?, ?, '0', '0', 'setup', CURRENT_TIMESTAMP)",
                username, nickName, UserConstants.USER_TYPE_SYSTEM, encrypted);
        executeUpdate(dataSource, "INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1)");
    }

    public void upsertConfig(DataSource dataSource, String key, String value, String name) {
        int count = queryCount(dataSource, "SELECT COUNT(*) FROM sys_config WHERE config_key = ?", key);
        if (count > 0) {
            executeUpdate(dataSource,
                    "UPDATE sys_config SET config_value = ?, update_by = 'setup', update_time = CURRENT_TIMESTAMP WHERE config_key = ?",
                    value, key);
            return;
        }
        executeUpdate(dataSource,
                "INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) "
                        + "VALUES (?, ?, ?, 'Y', 'setup', CURRENT_TIMESTAMP, '首次安装向导写入')",
                name, key, value);
    }

    public void markInstallCompleted(DataSource dataSource) {
        upsertConfig(dataSource, INSTALL_COMPLETED_KEY, "true", "安装完成");
    }

    public boolean isSchemaPresent(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return FlywaySchemaSupport.hasSuccessfulMigration(connection);
        } catch (Exception e) {
            return false;
        }
    }

    private static int queryCount(DataSource dataSource, String sql, String param) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void executeUpdate(DataSource dataSource, String sql, String... params) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
