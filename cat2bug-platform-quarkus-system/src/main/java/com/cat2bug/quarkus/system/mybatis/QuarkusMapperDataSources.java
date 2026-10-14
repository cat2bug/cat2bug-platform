package com.cat2bug.quarkus.system.mybatis;

/**
 * Quarkus MyBatis 命名数据源（与 {@code application.properties} 中 {@code quarkus.datasource.*} 对齐）。
 */
public final class QuarkusMapperDataSources {

    public static final String RUNTIME_MYSQL = "runtime-mysql";
    public static final String RUNTIME_H2 = "runtime-h2";

    /** 与 {@link io.quarkiverse.mybatis.runtime.QuarkusDatabaseIdProvider} 对齐的 config 键。 */
    public static String databaseIdConfigKey(String datasourceName) {
        return "quarkus.mybatis." + datasourceName + ".database-id";
    }

    private QuarkusMapperDataSources() {
    }
}
