package com.cat2bug.admin.setup;

import com.cat2bug.common.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SetupSubmitDataSourceFactory {

    public SimpleDataSource createMysqlDataSource(SetupSubmitRequest request) {
        SetupDatabaseTestRequest test = toDatabaseTestRequest(request);
        return createMysqlDataSource(test, request.getMysqlUsername(), request.getMysqlPassword());
    }

    public SimpleDataSource createH2DataSource(SetupSubmitRequest request) {
        String databaseName = DatabaseExistenceProbe.resolveH2DatabaseName(request);
        DatabaseExistenceProbe.assertValidDatabaseName(databaseName);
        return new SimpleDataSource(
                "org.h2.Driver",
                DatabaseExistenceProbe.resolveH2JdbcUrl(databaseName),
                "root",
                "cat2bug_password");
    }

    private SimpleDataSource createMysqlDataSource(SetupDatabaseTestRequest test, String username, String password) {
        return new SimpleDataSource(
                "com.mysql.cj.jdbc.Driver",
                SetupDatabaseTestService.resolveMysqlUrl(test),
                username,
                password);
    }

    private static SetupDatabaseTestRequest toDatabaseTestRequest(SetupSubmitRequest request) {
        SetupDatabaseTestRequest test = new SetupDatabaseTestRequest();
        test.setDatabaseType("mysql");
        test.setHost(request.getMysqlHost());
        test.setPort(request.getMysqlPort());
        test.setDatabase(request.getMysqlDatabase());
        test.setUsername(request.getMysqlUsername());
        test.setPassword(request.getMysqlPassword());
        return test;
    }

    public static boolean isMysql(SetupSubmitRequest request) {
        return request != null
                && StringUtils.isNotEmpty(request.getDatabaseType())
                && "mysql".equalsIgnoreCase(request.getDatabaseType());
    }
}
