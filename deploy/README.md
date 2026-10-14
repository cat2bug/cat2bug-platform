# Cat2Bug 部署脚本

后端只有一条线：Quarkus 模块 `cat2bug-platform-admin`，默认 Maven reactor 直接构建，无需任何 profile。

| 脚本 | 说明 |
|------|------|
| [`build-embedded.sh`](build-embedded.sh) | 前端 `build:embedded` + 后端单 JAR（`cat2bug-platform-admin/target/cat2bug-admin.jar`） |
| [`run-jar.sh`](run-jar.sh) | 启动单 JAR；自动加载 `config/install` 下的安装配置 |
| [`build-native.sh`](build-native.sh) | embedded SPA + Native ELF（容器内 Mandrel 构建）+ UPX |
| [`../cat2bug-platform-admin/smoke-native-docker.sh`](../cat2bug-platform-admin/smoke-native-docker.sh) | 在 Docker 中冒烟 Linux Native 二进制 |
| [`docker/build.sh`](docker/build.sh) | Docker Compose 前置：前端 `build:prod` + MySQL 版单 JAR |
| [`rpm/cat2bug/build-rpm.sh`](rpm/cat2bug/build-rpm.sh) | 用 nfpm 把 Native ELF 打成 rpm |
| [`upx-native-binary.sh`](upx-native-binary.sh) | 单独对 Native 二进制做 UPX 压缩 |
| [`scripts/export-public.sh`](scripts/export-public.sh) | 生成剔除内部资料的公开发布提交（`public/master`），只打印推送命令 |

## JVM 单 JAR

```bash
./deploy/build-embedded.sh
./deploy/run-jar.sh            # 默认端口 2020
```

`build-embedded.sh` 产出的 JAR 只内置 H2 驱动类型。数据源 `db-kind` 是 Quarkus 构建期属性，需要 MySQL 时按 mysql profile 打包、运行期切回 prod：

```bash
mvn -Pembedded -Prelease -Dquarkus.profile=mysql -pl cat2bug-platform-admin -am package -DskipTests
QUARKUS_PROFILE=prod java -jar cat2bug-platform-admin/target/cat2bug-admin-runner.jar
```

## Native

```bash
./deploy/build-native.sh               # 本机架构
./deploy/build-native.sh x86_64        # linux/amd64
./deploy/build-native.sh aarch64       # linux/arm64
SKIP_EMBEDDED=true ./deploy/build-native.sh   # 跳过前端（API-only）

./cat2bug-platform-admin/smoke-native-docker.sh arm64
FRESH=true ./cat2bug-platform-admin/smoke-native-docker.sh arm64   # 空数据目录走安装向导
```

产物：`cat2bug-platform-admin/target/cat2bug-admin-linux-<goarch>`，UPX 后为同名 `.upx`。

**amd64 在 Apple Silicon 上**：Docker `linux/amd64` 交叉编译内存占用高，`native-image` 若以 exit 137 结束，请调高 Docker Desktop 内存（建议 ≥16GB），或在 Linux x86_64 主机构建。

## Docker Compose

见 [`docker/README.md`](docker/README.md)。

## RPM

```bash
./deploy/build-native.sh x86_64
./deploy/rpm/cat2bug/build-rpm.sh x86_64      # 产物 dist/*.rpm；RPM_USE_UPX=true 改用 .upx
```

## 公开发布

内部资料（`docs/`、`openspec/` 等，见 [`public-release-exclude.txt`](public-release-exclude.txt)）只进私有远端。发布公开仓库时：

```bash
./deploy/scripts/export-public.sh   # 更新本地 public/master，打印推送命令，不推送
```

开源项目不维护 CI；[`ci/native.yml`](ci/native.yml) 仅作自建流水线参考。
