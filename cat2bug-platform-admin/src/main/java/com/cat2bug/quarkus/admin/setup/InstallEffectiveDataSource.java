package com.cat2bug.quarkus.admin.setup;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.DataSources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;

/**
 * 解析当前生效的数据源：安装后激活 {@code runtime-mysql}（MySQL 或 H2 文件库），否则使用 default（bootstrap H2）。
 * 通过 {@link DataSources} 直接取池，避免与 {@link InstallDefaultDataSourceBridge} 的 {@code @Default} 产生循环依赖。
 */
@ApplicationScoped
public class InstallEffectiveDataSource {

    /** Quarkus 默认数据源在 Agroal 内部 registry 中的 key */
    private static final String DEFAULT_DATASOURCE = "";

    @ConfigProperty(name = "quarkus.datasource.runtime-mysql.active", defaultValue = "false")
    boolean runtimeMysqlActive;

    /** 旧版 install 曾激活 runtime-h2；新装 H2 亦走 runtime-mysql，此处仅作兼容。 */
    @ConfigProperty(name = "quarkus.datasource.runtime-h2.active", defaultValue = "false")
    boolean runtimeH2Active;

    @Inject
    DataSources dataSources;

    public DataSource get() {
        if (runtimeMysqlActive) {
            return dataSources.getDataSource(InstallDatasourceNames.RUNTIME_MYSQL);
        }
        if (runtimeH2Active) {
            return dataSources.getDataSource(InstallDatasourceNames.RUNTIME_H2);
        }
        return dataSources.getDataSource(DEFAULT_DATASOURCE);
    }
}
