package com.cat2bug.web.service.setup;

import com.cat2bug.common.constant.FlywayConstants;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.service.UpgradeMigrationInspector;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * 安装/升级阶段执行 Flyway 迁移。
 */
@Service
public class SetupMigrationService implements UpgradeMigrationInspector
{
    @Autowired
    private DataSource dataSource;

    public void migrate(String databaseType, DataSource dataSourceOverride)
    {
        String type = StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2";
        try
        {
            buildFlyway(type, dataSourceOverride != null ? dataSourceOverride : dataSource).migrate();
        }
        catch (FlywayException e)
        {
            throw new ServiceException(SetupMessages.msg("setup.install.migration.failed", e.getMessage()));
        }
    }

    public void repairIfChecksumConflict(String databaseType, DataSource dataSourceOverride, String errorMessage)
    {
        if (errorMessage == null || !errorMessage.toLowerCase().contains("checksum"))
        {
            return;
        }
        String type = StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2";
        buildFlyway(type, dataSourceOverride != null ? dataSourceOverride : dataSource).repair();
    }

    @Override
    public List<String> listPendingMigrations(String databaseType)
    {
        String type = StringUtils.isNotEmpty(databaseType) ? databaseType.toLowerCase() : "h2";
        try
        {
            MigrationInfoService info = buildFlyway(type, dataSource).info();
            List<String> pending = new ArrayList<>();
            for (MigrationInfo migration : info.pending())
            {
                pending.add(migration.getVersion() + " " + migration.getDescription());
            }
            return pending;
        }
        catch (Exception e)
        {
            return List.of();
        }
    }

    @Override
    public boolean hasPendingMigrations(String databaseType)
    {
        return !listPendingMigrations(databaseType).isEmpty();
    }

    private Flyway buildFlyway(String databaseType, DataSource dataSourceOverride)
    {
        var config = Flyway.configure()
                .locations("classpath:db/migration/" + databaseType)
                .table(FlywayConstants.SCHEMA_HISTORY_TABLE)
                .encoding("UTF-8")
                .baselineOnMigrate(true)
                .baselineVersion("0.5.0")
                .baselineDescription("BaseLineInitialize")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .dataSource(dataSourceOverride != null ? dataSourceOverride : dataSource);
        return config.load();
    }
}
