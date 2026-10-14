# Quarkus Mailer 的 host 默认 localhost

- 日期：2026-10-14
- 涉及：后台 / 部署

## 现象

用 `@ConfigProperty(name = "quarkus.mailer.host") Optional<String> host` 判断「邮件是否已配置」，即使从没设置过 SMTP 主机，`host` 也总是有值，判空不生效。开启邮件但漏配主机时，每次发信都去连本机 `localhost:25`，连接失败只体现在 WARN 日志里。

## 根因

`quarkus-mailer` 扩展给 `quarkus.mailer.host` 内置了默认值 `localhost`（端口也有默认值），MicroProfile Config 注入时拿到的是这个默认值，而不是「未设置」。

## 解决办法

- 「是否已配置」以自有开关 `cat2bug.mail.enabled` 和没有默认值的 `quarkus.mailer.from` 为准，`host` 只作辅助判断。
- 文档与部署说明把 `QUARKUS_MAILER_HOST` 列为必填，并提示漏配的表现是 WARN「连接 localhost 失败」。

## 以后怎么避免

- 用扩展自带的配置项判断「用户是否配置」前，先查该扩展的配置参考页确认它有没有默认值；有默认值的项不能当作开关。
- 需要严格区分「未设置」时，引入自有开关属性，或用 `ConfigProvider.getConfig().getConfigValue(name).getSourceName()` 判断值来源。

相关：[[IM多渠道推送]]
