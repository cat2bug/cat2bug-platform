package com.cat2bug.web.service.setup;

import com.alibaba.druid.pool.DruidDataSource;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.springframework.stereotype.Component;

/**
 * 安装提交时按向导所选数据库创建临时数据源（引导模式下运行时库可能仍为 H2）。
 */
@Component
public class SetupSubmitDataSourceFactory
{
    public DruidDataSource createMysqlDataSource(SetupSubmitRequest request)
    {
        SetupDatabaseTestRequest test = toDatabaseTestRequest(request);
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(SetupDatabaseTestService.resolveMysqlUrl(test));
        dataSource.setUsername(request.getMysqlUsername());
        dataSource.setPassword(request.getMysqlPassword());
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setMaxActive(3);
        dataSource.setMaxWait(30000);
        dataSource.setConnectTimeout(30000);
        dataSource.setSocketTimeout(60000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(false);
        dataSource.setTestOnBorrow(false);
        return dataSource;
    }

    private static SetupDatabaseTestRequest toDatabaseTestRequest(SetupSubmitRequest request)
    {
        SetupDatabaseTestRequest test = new SetupDatabaseTestRequest();
        test.setDatabaseType("mysql");
        test.setHost(request.getMysqlHost());
        test.setPort(request.getMysqlPort());
        test.setDatabase(request.getMysqlDatabase());
        test.setUsername(request.getMysqlUsername());
        test.setPassword(request.getMysqlPassword());
        return test;
    }

    public static boolean isMysql(SetupSubmitRequest request)
    {
        return request != null
                && StringUtils.isNotEmpty(request.getDatabaseType())
                && "mysql".equalsIgnoreCase(request.getDatabaseType());
    }
}
