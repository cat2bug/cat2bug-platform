# Gate B — Quarkus Native 发布说明

## 实测结果（2026-10-14，linux/arm64，Mandrel jdk-21 容器构建）

| 项 | POI 版（第一轮） | FastExcel 版（第二轮） | 前端瘦身后 | **对等补齐后（当前）** |
|---|---|---|---|---|
| 原始 ELF `cat2bug-admin-linux-arm64` | 270,607,312 B（258 MiB） | 185,738,192 B（177 MiB） | 174,793,680 B（167 MiB） | **177,153,008 B（169 MiB）** |
| UPX `--best` 后 `cat2bug-admin-linux-arm64.upx` | 62,386,824 B（≈60 MiB） | 48,852,108 B（≈46.6 MiB） | 40,317,680 B（≈38.4 MiB） | **40,694,688 B（≈38.8 MiB）** |
| 代码区 / 镜像堆 / 其它 | 94.0 / 163.6 / 12 MB | 64.8 / 111.8 / 10.6 MB | — | — |
| 镜像堆中嵌入资源 | ≈35 MB（含 OOXML schema、docs） | 29.8 MB（含 target 残留旧前端文件） | 19.6 MB（SPA ≈17 MB） | — |
| 目标 | ~20 MB（UPX 后口径） | 未达成 | 未达成 | **产品负责人已接受 ≈38 MiB** |

第二轮瘦身（不裁功能）：

- Excel 全部改用 FastExcel（写）+ fastexcel-reader（读），移除 POI / poi-ooxml-full / XmlBeans / Saxon-HE / brotli；POI 仅保留为 `test` 依赖，用于独立回读校验产物。
- FastExcel 没有图表 API：新增 `XlsxPatcher` 在 xlsx 包上注入原生 drawing + chart 部件（纯 JDK zip + 字符串），恢复缺陷/成员走势折线图、测试计划统计 4 张柱图（锚点、配色、平滑/标记、数据标签与原 POI 版一致，含缓存值）。
- Quarkus 嵌入包不再携带帮助文档 `/docs`（已迁至官网）。

第三轮：前端瘦身（不裁功能）。SPA 原始 21 MB → 17 MB，xz 口径 7.9 MB → 5.2 MB；首屏 JS/CSS ≈12 MB → ≈4.2 MB：

- 删除未使用的 jsPDF 宋体字体 `simsun-normal.js`（1.3 MB；PDF 导出是截图写图片，从不输出文字）。
- vendored `echarts.min.js` 整包（5.6.0，1 MB）改为 npm `echarts@5.6.0` 按需注册（`src/utils/echarts.js`，新增图表类型需在此 `use()`）。
- PNG 经 pngquant + oxipng（2.69 → 1.41 MB）；项目封面 SVG 经 svgo（1.52 → 0.56 MB，渲染 RMSE < 0.3%）；提示音转单声道 64 kbps（446 → 218 KB）。
- KaTeX 字体只保留 woff2（vite 插件 `katex-woff2-only` 去掉 ttf/woff 副本）。
- 分包：不再把全部 node_modules / src/components 各打成一个 5 MB 包；只固定 `chunk-vendor`（vue、element-plus 及其传递依赖，避免循环 chunk 导致 TDZ 白屏），其余随路由懒加载；非默认语言包按需加载；jsPDF 在导出时动态加载。
- 构建脚本在 mvn 前清空 `target/classes/META-INF/resources`：Maven 增量复制不删除旧 hash 文件，此前残留会被一并嵌入（第二轮的 29.8 MB 即含残留）。

对等补齐（定时任务 quarkus-scheduler、flyway-mysql 与 `db/mysql` 迁移脚本、操作日志、监控等）使 UPX 后体积增加约 0.4 MiB。

amd64 未在本机构建；命令相同（`./deploy/build-native-quarkus.sh amd64`），体积预计同量级。

## 构建与运行

```bash
# 前端 embedded（build:embedded-quarkus → META-INF/resources）+ Native + UPX
./deploy/build-native-quarkus.sh arm64          # 或 amd64
# 可选：SKIP_EMBEDDED=true 复用已有前端产物；SKIP_UPX=true 跳过压缩

# 产物
cat2bug-platform-quarkus-admin/target/cat2bug-admin-linux-arm64       # 原始 ELF
cat2bug-platform-quarkus-admin/target/cat2bug-admin-linux-arm64.upx   # 默认分发

# 运行（工作目录即数据目录：config/install、data、upload 等均相对于此）
mkdir -p /opt/cat2bug && cd /opt/cat2bug
./cat2bug-admin-linux-arm64.upx            # 首次启动进入安装向导 http://<host>:2020/
```

- 安装向导完成后进程自行退出，需由 systemd / Docker `restart` 策略拉起；重启后自动加载 `config/install/quarkus-runtime.properties`。
- H2 相对路径在写入运行期配置时会解析为绝对路径（Native 下 H2 的相对路径基准不可靠）。
- 持久化：整个工作目录（含 `data/`、`upload/`、`git/` bare 仓、`config/install/`）都需要挂载卷或做备份。

## 冒烟（Docker 内运行 Linux ELF）

```bash
# 复用 Gate A 的 smoke 库（/tmp/cat2bug-quarkus-smoke）
./cat2bug-platform-quarkus-admin/smoke-native-docker.sh arm64
# 全新安装：空目录 → /setup/submit(H2) → Flyway → 重启 → 登录 → 业务
FRESH=true ./cat2bug-platform-quarkus-admin/smoke-native-docker.sh arm64
# 排障：KEEP_CONTAINER=true 保留容器
```

Excel 检查项：用例/缺陷/用户导出、用例/缺陷/用户导入模板、用例导入、缺陷状态走势与成员走势（有数据行时必须含图表）、测试计划统计（必须含 4 张图）。两种模式均已通过（详见 `SMOKE-RESULTS.md` Gate B 一节）。

## Native 适配要点

| 问题 | 处理 |
|---|---|
| JJWT 反射加载 `io.jsonwebtoken.impl.*`、ServiceLoader 找 Serializer | `NativeRuntimeInitFeature` 注册反射；`TokenService` 显式 `.json(JacksonSerializer/Deserializer)`；`io.jsonwebtoken` 运行期初始化 |
| MyBatis / Jackson 用到的 domain/VO 反射 | Feature 按包前缀（`com.cat2bug.quarkus.common.*` 等）整类注册 |
| POI / XmlBeans 反射、ServiceLoader、schema 资源 | **已整体移除**（改 FastExcel，无反射/无 ServiceLoader） |
| FastExcel 输出的 zip 本地头大小为 0（Zip64 数据描述符） | `XlsxPatcher` 经 `ZipFile` 中央目录读取，不用 `ZipInputStream` |
| i18n、安装模板、报告预置模版 | `quarkus.native.resources.includes` 补齐 |
| JGit `Config` 反射调用枚举 `values()` | JGit 枚举整类注册 |
| Flyway classpath Scanner 在 Native 被替换 | `SetupMigrationService` 在 Native 下改用 Quarkus 构建期索引的迁移文件 |
| MySQL 迁移脚本与 H2 脚本同在 `db/migration/*` 下时，Native 的 Flyway 扫描按前缀匹配会把两套脚本混在一起 | MySQL 脚本移到 `db/mysql`，`SetupMigrationService.migrationLocation` 按数据库产品名选择目录 |
| 安装后运行期配置未被自动加载 | 恢复 `META-INF/services/...ConfigSource`（`InstallRuntimePropertiesConfigSource`） |
| vite docs 复制到错误目录 | `viteStaticCopy` 的 `dest` 相对 `outDir`；Quarkus 嵌入构建不再复制 docs |

## 行为差异（FastExcel 迁移）

- 导入只支持 `.xlsx`（POI 时理论上可读 `.xls`）；前端模版与导出均为 xlsx，正常流程不受影响。
- 自定义字段遇到科学计数格式、正负两段格式的数字单元格时，按常规格式输出文本（如 `1.5`，原为 `1.50E+00`）。
- 条件格式“≤0 标红”改用等价的表达式规则 `0>=B3`（FastExcel 不转义 `<`）。

## 与 ~20MB 目标的偏差

功能对等（Excel 全量含图表、Git、H2+MySQL 同包、嵌入 SPA 单文件）优先于体积，按 design D3「禁止以体积裁功能」执行。当前主要体积来源：

| 来源 | 原始体积 |
|---|---|
| 嵌入前端 SPA | ≈17 MB 资源（xz 口径 ≈5.2 MB；其中 PNG/mp3/woff2 等已压缩资源 UPX 无收益） |
| java.base + java.xml | ≈17 MB 代码 |
| H2 + MySQL 驱动同包 | ≈7.8 MB 代码 |
| JGit / fastjson2 / Jackson / Hutool 等 | ≈12 MB 代码 |
| 代码元数据 / 字符串 | ≈40 MB 镜像堆 |

继续缩小的方向（均需产品决策）：

1. 前端剩余可裁项均涉及功能取舍：Markdown 代码块高亮的全语言包（codemirror/lezer，xz ≈0.45 MB）收窄为常用语言；7 种界面语言（已懒加载，xz ≈0.16 MB）。
2. 按数据库拆分 H2 / MySQL 两个 Native 包（UPX 后约 −2~3 MB）。
3. fastjson2 统一为 Jackson（约 −1~2 MB）。
4. SPA 不嵌入二进制，改为同目录静态资源（偏离「单文件」形态，UPX 后约 −5 MB）。

在保留 JGit、双库、嵌入 SPA 的前提下，当前 ≈38.8 MiB 已接近下限；~20 MB 需要拆包或不嵌入 SPA。
