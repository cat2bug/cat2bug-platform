# cat2bug-platform-quarkus-spike（已废弃）

> **DEPRECATED**：代码已迁移至 `cat2bug-platform-quarkus-admin` / `quarkus-framework` / `quarkus-common`。
> 请使用 Maven profile **`-Pquarkus`**，不再使用本目录下的源码。

## 替代命令

```bash
# 开发与测试
mvn -pl cat2bug-platform-quarkus-admin -Pquarkus -am quarkus:dev
mvn -pl cat2bug-platform-quarkus-admin -Pquarkus -am test

# Native（Docker）
./cat2bug-platform-quarkus-admin/build-native-docker.sh arm64
```

历史 Spike 报告见 `readme/quarkus-migration-spike/REPORT.md`。
