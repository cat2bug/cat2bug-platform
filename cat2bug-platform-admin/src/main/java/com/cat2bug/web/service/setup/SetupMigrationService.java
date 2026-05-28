package com.cat2bug.web.service.setup;

import com.cat2bug.common.constant.FlywayConstants;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.StringUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * 安装阶段执行 Flyway 迁移（与 application.yml 中 flyway 配置对齐）
 */
@Service
public class SetupMigrationService
{
    public void migrate(String databaseType, DataSource dataSource)
    {
        String type = StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2";
        try
        {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/" + type)
                    .table(FlywayConstants.SCHEMA_HISTORY_TABLE)
                    .encoding("UTF-8")
                    .baselineOnMigrate(true)
                    .baselineVersion("0.5.0")
                    .baselineDescription("BaseLineInitialize")
                    .validateOnMigrate(true)
                    .outOfOrder(false)
                    .load();
            flyway.migrate();
        }
        catch (FlywayException e)
        {
            throw new ServiceException(SetupMessages.msg("setup.install.migration.failed", e.getMessage()));
        }
    }
}
