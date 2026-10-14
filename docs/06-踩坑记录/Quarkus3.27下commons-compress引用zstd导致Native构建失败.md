# Quarkus 3.27 下 commons-compress 引用 zstd 导致 Native 构建失败

升级 Quarkus BOM 后 JVM 正常、Native 构建期报 zstd 类找不到的原因与处理方式。

- 日期：2026-10-15
- 涉及：后台 / 部署

## 现象

Quarkus 从 3.15.7 升到 3.27.6 后，JVM 测试全部通过，`deploy/build-native.sh` 在 Native 分析阶段失败：

```
NoClassDefFoundError: com/github/luben/zstd/ZstdInputStream
```

## 根因

Quarkus 3.27 BOM 把 commons-compress 从 1.27.0 升到 1.28.0。新版 `ZipFile.createZstdInputStream` 直接引用 zstd-jni（可选依赖，项目没有引入）。JVM 下只有真正读取 zstd 压缩条目时才加载该类，所以不出错；Native 构建期要静态链接所有可达方法，于是失败。

## 解决办法

新增 `cat2bug-platform-admin/src/main/java/com/cat2bug/admin/nativeimage/Target_CommonsCompressZipFile.java`，用 GraalVM `@TargetClass` / `@Substitute` 把 `createZstdInputStream` 替换为直接抛 `IOException`。项目不读取 zstd 压缩的 zip，不影响功能；没有把 commons-compress 钉回旧版本，以免与 BOM 其他组件不一致。

## 以后怎么避免

- 升级 Quarkus（或其他 BOM）后必须重建一次 Native，JVM 测试通过不代表 Native 能构建。
- 遇到可选依赖导致的构建期 `NoClassDefFoundError`，优先用 substitution 切断不用的代码路径，而不是引入新库或回钉版本。
- 再次升级 commons-compress 时检查该替换的方法签名是否还存在，否则构建会报替换目标不存在。
