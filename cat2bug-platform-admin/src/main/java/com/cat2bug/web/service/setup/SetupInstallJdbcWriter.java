package com.cat2bug.web.service.setup;

import com.cat2bug.common.constant.UserConstants;
import com.cat2bug.common.utils.FlywaySchemaSupport;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.service.InstallService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * 安装阶段写入外部 MySQL（引导模式下 Spring 仍连接 H2，需直连目标库）。
 */
@Component
public class SetupInstallJdbcWriter
{
    public void createOrUpdateAdmin(DataSource dataSource, SetupSubmitRequest request)
    {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String username = request.getAdminUsername().trim();
        String encrypted = SecurityUtils.encryptPassword(request.getAdminPassword());
        String nickName = StringUtils.isNotEmpty(request.getAdminNickName()) ? request.getAdminNickName() : username;

        List<Long> userIds = jdbc.query(
                "SELECT user_id FROM sys_user WHERE user_name = ? AND del_flag = '0'",
                (rs, rowNum) -> rs.getLong(1),
                username);
        if (!userIds.isEmpty())
        {
            jdbc.update(
                    "UPDATE sys_user SET password = ?, nick_name = ?, update_by = 'setup', update_time = NOW() WHERE user_id = ?",
                    encrypted, nickName, userIds.get(0));
            return;
        }

        jdbc.update(
                "INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, password, status, del_flag, create_by, create_time) "
                        + "VALUES (0, ?, ?, ?, ?, '0', '0', 'setup', NOW())",
                username, nickName, UserConstants.USER_TYPE_SYSTEM, encrypted);
        Long userId = jdbc.queryForObject(
                "SELECT user_id FROM sys_user WHERE user_name = ? AND del_flag = '0'",
                Long.class,
                username);
        Integer roleCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE user_id = ? AND role_id = 1",
                Integer.class,
                userId);
        if (roleCount == null || roleCount == 0)
        {
            jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, 1)", userId);
        }
    }

    public void upsertConfig(DataSource dataSource, String key, String value, String name)
    {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_config WHERE config_key = ?",
                Integer.class,
                key);
        if (count != null && count > 0)
        {
            jdbc.update(
                    "UPDATE sys_config SET config_value = ?, update_by = 'setup', update_time = NOW() WHERE config_key = ?",
                    value, key);
            return;
        }
        jdbc.update(
                "INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) "
                        + "VALUES (?, ?, ?, 'Y', 'setup', NOW(), '首次安装向导写入')",
                name, key, value);
    }

    public void markInstallCompleted(DataSource dataSource)
    {
        upsertConfig(dataSource, InstallService.INSTALL_COMPLETED_KEY, "true", "安装完成");
    }

    public boolean isSchemaPresent(DataSource dataSource)
    {
        return FlywaySchemaSupport.hasSuccessfulMigration(new JdbcTemplate(dataSource));
    }
}
