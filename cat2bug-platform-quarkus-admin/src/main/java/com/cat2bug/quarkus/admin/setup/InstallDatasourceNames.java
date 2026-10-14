package com.cat2bug.quarkus.admin.setup;

/**
 * Quarkus 命名数据源：MyBatis build-time 固定 {@link #RUNTIME_MYSQL}；H2/MySQL 安装均激活该池并覆盖 JDBC URL。
 */
public final class InstallDatasourceNames {

    public static final String RUNTIME_MYSQL = "runtime-mysql";
    public static final String RUNTIME_H2 = "runtime-h2";

    private InstallDatasourceNames() {
    }
}
