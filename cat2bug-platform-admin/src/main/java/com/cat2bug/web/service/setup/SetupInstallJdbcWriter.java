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

        // 删除已存在的超管用户(userId = 1)或任何同名用户，以保证新建的超管用户不冲突
        jdbc.update("DELETE FROM sys_user WHERE user_id = 1 OR user_name = ?", username);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = 1");

        // 插入固定 userId = 1 的全新超管用户
        jdbc.update(
                "INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, password, status, del_flag, create_by, create_time) "
                        + "VALUES (1, 0, ?, ?, ?, ?, '0', '0', 'setup', NOW())",
                username, nickName, UserConstants.USER_TYPE_SYSTEM, encrypted);

        // 绑定系统管理员角色 (role_id = 1)
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1)");
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
        return FlywaySchemaSupport.hasCoreSchema(new JdbcTemplate(dataSource));
    }
}
