# Gate A 验收说明

## 验收状态

| 项 | 状态 |
|---|---|
| Gate A（Quarkus JVM 功能/UI） | **已通过**（2026-10-14，产品负责人手动确认） |
| Gate B（Native ~20MB） | 功能冒烟通过；体积 UPX 后 ≈38.8 MiB（POI→FastExcel、去 docs、前端瘦身后由 ≈60 MiB 降下），未达 ~20MB，**产品负责人已接受该体积**（见 `NATIVE-RELEASE.md`） |

## 本阶段范围（已交付）

- Quarkus 为默认后端（JVM）
- 核心业务 + Excel 全量对等 + Git/源码管理 API 迁入
- 自动化冒烟 + 浏览器复测见 `SMOKE-RESULTS.md`

## 如何启动验收环境

```bash
# 开发热重载
./cat2bug-platform-quarkus-admin/dev.sh
# 或
mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am quarkus:dev

# 已打包 JVM 产物（fast-jar）
java -jar cat2bug-platform-quarkus-admin/target/quarkus-app/quarkus-run.jar
# 或
./deploy/run-quarkus-jar.sh
```

本地配置：`LOCAL-RUN.md` · 门禁：`GATE-NOTES.md` · 冒烟：`SMOKE-RESULTS.md`

## Gate A 后的对等补齐（2026-10-14）

全量对比 Spring Controller 与 Quarkus Resource 端点后补齐的缺口见 `tasks.md` §7。需要注意的行为差异：

- 数据监控页（`/druid/*`）显示的是 Agroal 连接池，而非 Druid 原版控制台。
- `user/config` 增删比 Spring 更严格：非管理员只能新增/删除本人的配置。
- 为与 Quarkus 的两个独有迁移脚本对齐，Spring 的 `db/migration/{h2,mysql}` 同样新增了 `V0_6_2_16_1`、`V0_6_2_16_2`（隐藏岗位菜单、浏览器工具菜单），并开启 `outOfOrder`。
- Quarkus 的 MySQL 迁移脚本位于 `classpath:db/mysql`（H2 仍为 `db/migration`）。
- 运行期配置须为新格式（`quarkus.datasource.runtime-mysql.*` + `quarkus.datasource.active=false`），旧格式（只写默认数据源）会导致登录 500：`Datasource 'runtime-mysql' was deactivated`。
- 测试库为 `cat2bug_platform_test`，与开发库分离。

## 已知说明

- H2 新装写入 `db-kind=h2` + `jdbc.driver=org.h2.Driver`
- **打包默认 H2**；MySQL 须 `-Dquarkus.profile=mysql` 再 package
- 前端开发：`cat2bug-platform-ui` → http://127.0.0.1:2222/ 代理 :2020
