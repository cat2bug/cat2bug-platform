package com.cat2bug.framework.service;

import java.util.List;

/**
 * 由 admin 模块提供 Flyway 待执行迁移检测（framework 不直接依赖 flyway-core）。
 */
public interface UpgradeMigrationInspector
{
    List<String> listPendingMigrations(String databaseType);

    boolean hasPendingMigrations(String databaseType);
}
