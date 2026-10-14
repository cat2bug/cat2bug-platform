# H2 中 CAST AS CHAR 不带长度会截成一个字符

同一句 SQL 在 MySQL 上正常，在 H2（Native / 单 JAR 默认库）上只剩第一个字符。

- 日期：2026-10-14
- 涉及：后台 / 部署

## 现象

- 通知详情的正文只显示一个字（如「X」），列表接口返回的 `noticeContent` 本身就只有一个字符。
- 仪表盘动态里 ID 和状态被截断：ID 10、11 变成 `1`，状态 `PROCESSING` 变成 `P`；按用户名关联创建人时只比较首字符。
- 跑在 MySQL 测试库上的集成测试全部通过，发现不了。

## 根因

MyBatis XML 里写了 `CAST(x AS CHAR)`。MySQL 把不带长度的 `CHAR` 当作变长字符串；H2 按 SQL 标准把它当作 `CHAR(1)`，结果被截成一个字符。

## 解决办法

- 列本身就是文本类型（如 `longtext`）时直接去掉 cast。
- 确实需要转成字符串时写明长度：`CAST(x AS CHAR(255))`，长文本用 `CHAR(3000)`。两种数据库都支持，结果一致。

## 以后怎么避免

- Mapper XML 里不要出现不带长度的 `AS CHAR)`，提交前用 `rg -n -i "as char\)" cat2bug-platform-admin/src/main/resources/mapper` 检查。
- 集成测试连的是 MySQL 测试库，H2 方言差异只能靠 Native 冒烟或 H2 实库发现；改 SQL 后至少跑一次 `FRESH=true ./cat2bug-platform-admin/smoke-native-docker.sh arm64`，见 [[Quarkus数据源db-kind是构建期属性]]。
