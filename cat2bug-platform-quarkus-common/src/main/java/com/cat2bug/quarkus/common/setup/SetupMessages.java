package com.cat2bug.quarkus.common.setup;

import java.text.MessageFormat;
import java.util.Map;

/**
 * 安装向导 API 文案（中文，与 Spring messages.properties 默认文案一致）。
 */
public final class SetupMessages {

    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("setup.install.ai.host.required", "启用 AI 时必须填写 Ollama 地址"),
            Map.entry("setup.install.already", "系统已完成安装"),
            Map.entry("setup.install.cache.unsupported", "不支持的缓存类型"),
            Map.entry("setup.install.completed", "安装完成，请重启应用使数据库、缓存与路径配置生效"),
            Map.entry("setup.install.database.mode.invalid", "数据库模式无效，必须为 new 或 existing"),
            Map.entry("setup.install.database.mode.mismatch", "数据库模式与当前库状态不一致，请重新测试连接"),
            Map.entry("setup.install.h2.existing.empty", "H2 数据库文件已存在但未初始化表结构，请删除 data 目录下对应 .mv.db 文件后重新测试连接，或选择新建库"),
            Map.entry("setup.install.database.mode.required", "请选择数据库模式（new 或 existing）"),
            Map.entry("setup.install.db.type.required", "请选择数据库类型"),
            Map.entry("setup.install.db.type.unsupported", "不支持的数据库类型"),
            Map.entry("setup.install.migration.failed", "数据库初始化失败: {0}"),
            Map.entry("setup.install.mysql.incomplete", "MySQL 连接信息不完整"),
            Map.entry("setup.install.params.empty", "安装参数不能为空"),
            Map.entry("setup.install.path.label.log", "日志目录"),
            Map.entry("setup.install.path.label.profile", "文件存储路径"),
            Map.entry("setup.install.path.label.temp", "临时目录"),
            Map.entry("setup.install.path.unavailable", "{0}不可用: {1}"),
            Map.entry("setup.install.redis.incomplete", "Redis 连接信息不完整"),
            Map.entry("setup.install.redis.test.failed", "Redis 连接测试未通过: {0}"),
            Map.entry("setup.install.schema.missing", "数据库初始化失败，未检测到表结构，请查看日志中的 Flyway 迁移错误"),
            Map.entry("setup.mysql.connection.invalid", "MySQL 连接无效"),
            Map.entry("setup.mysql.create.failed", "创建 MySQL 数据库失败: {0}"),
            Map.entry("setup.mysql.detect.failed", "检测 MySQL 数据库是否存在失败: {0}"),
            Map.entry("setup.mysql.schema.existing.hint", "目标数据库已存在且含有 Flyway 迁移记录，将在此库上写入安装配置，不会重复执行迁移，也不会修改已有管理员账号。"),
            Map.entry("setup.test.connection.failed", "{0}"),
            Map.entry("setup.test.connection.invalid", "连接无效"),
            Map.entry("setup.test.connection.success", "连接成功"),
            Map.entry("setup.test.database.access.denied", "连接被拒绝，请检查数据库用户名和密码"),
            Map.entry("setup.test.database.connect.failed", "无法建立数据库连接，请检查网络、防火墙与服务状态"),
            Map.entry("setup.test.database.h2.format.mismatch", "检测到 data 目录下存在旧版 H2 1.x 数据库文件，与当前内置 H2 2.x 不兼容。若无需保留数据，请停止服务后删除 data 目录中对应 *.mv.db 及同名 .trace.db、.lock.db 文件，再重新测试"),
            Map.entry("setup.test.database.mysql.server.ready", "MySQL 服务器连接成功，目标库尚未创建，完成安装时将自动建库并执行数据库迁移"),
            Map.entry("setup.test.database.name.empty", "请填写数据库名"),
            Map.entry("setup.test.database.name.invalid", "数据库名仅允许字母、数字与下划线"),
            Map.entry("setup.test.database.refused", "无法连接到数据库服务器，请确认 MySQL 已启动且主机、端口正确"),
            Map.entry("setup.test.database.type.empty", "数据库类型不能为空"),
            Map.entry("setup.test.database.type.unsupported", "不支持的数据库类型: {0}"),
            Map.entry("setup.test.database.unknown.database", "目标数据库尚未创建；完成安装时将自动建库并执行数据库迁移"),
            Map.entry("setup.test.ollama.connection.failed", "连接失败: {0}"),
            Map.entry("setup.test.ollama.host.empty", "Ollama 地址不能为空"),
            Map.entry("setup.test.path.empty", "路径不能为空"),
            Map.entry("setup.test.path.not.writable", "目录不可写: {0}"),
            Map.entry("setup.test.path.writable", "目录可写"),
            Map.entry("setup.test.redis.auth.required", "Redis 需要密码，请填写正确的密码"),
            Map.entry("setup.test.redis.auth.wrong", "Redis 密码错误，请检查后重试"),
            Map.entry("setup.test.redis.connect.failed", "无法连接到 Redis，请检查服务状态与连接信息"),
            Map.entry("setup.test.redis.dns.failed", "无法连接到 Redis，请确认服务已启动，且主机、端口填写正确"),
            Map.entry("setup.test.redis.host.empty", "Redis 主机与端口不能为空"),
            Map.entry("setup.test.redis.host.unknown", "无法解析 Redis 地址，请检查主机名是否填写正确"),
            Map.entry("setup.test.redis.ping.invalid", "PING 响应异常: {0}"),
            Map.entry("setup.test.redis.refused", "无法连接到 Redis，请确认服务已启动且主机、端口正确"),
            Map.entry("setup.test.redis.timeout", "连接 Redis 超时，请检查地址、端口、防火墙或网络是否可达"),
            Map.entry("upgrade.auth.required", "请提供系统管理员账号和密码"),
            Map.entry("upgrade.auth.denied", "管理员账号或密码不正确"),
            Map.entry("upgrade.auth.not.admin", "仅系统管理员可执行升级操作"),
            Map.entry("upgrade.backup.dir.failed", "无法创建备份目录: {0}"),
            Map.entry("upgrade.backup.file.invalid", "备份文件名无效，仅允许字母、数字、下划线、连字符，且必须以 .sql 结尾"),
            Map.entry("upgrade.backup.file.missing", "备份文件不存在或不可读"),
            Map.entry("upgrade.backup.mysql.failed", "MySQL 备份失败"),
            Map.entry("upgrade.backup.h2.failed", "H2 备份失败: {0}"),
            Map.entry("upgrade.rollback.mysql.failed", "MySQL 回滚失败"),
            Map.entry("upgrade.rollback.h2.failed", "H2 回滚失败: {0}"),
            Map.entry("upgrade.rollback.no.backup", "没有可用的升级备份文件，无法回滚"),
            Map.entry("upgrade.rollback.completed", "数据库已回滚到升级前备份，请确认数据后重新执行升级")
    );

    private SetupMessages() {
    }

    public static String msg(String code, Object... args) {
        String template = MESSAGES.getOrDefault(code, code);
        if (args == null || args.length == 0) {
            return template;
        }
        return MessageFormat.format(template, args);
    }
}
