# H2 FullTextLucene 缺 Lucene 导致 Native 构建失败

Quarkus JDBC H2 在 Native 构建期删除 `FullTextLucene` 时仍解析其方法签名，classpath 无 Lucene 即失败。

- 日期：2026-10-16
- 涉及：后台 / 部署

## 现象

本机 GraalVM 21 / Mandrel 构建 Native 时，初始化阶段即失败：

```
java.lang.NoClassDefFoundError: org/apache/lucene/index/IndexFormatTooOldException
```

堆栈落在 `AnnotationSubstitutionProcessor.handleDeletedClass`（处理 `@Delete` 的 `org.h2.fulltext.FullTextLucene`）。

## 根因

`quarkus-jdbc-h2` 通过 `DeleteFullTextLucene` 在 Native 中删除 H2 的 Lucene 全文检索实现。GraalVM 删除类前仍会 `getDeclaredMethods`，从而加载方法签名与 catch 类型里的 Lucene 类；项目未引入 `lucene-core`，于是构建失败。运行时并不使用 H2 Lucene 全文检索。

## 解决办法

在 `cat2bug-platform-admin` 的 `native` profile 中增加构建期依赖（仅 `-Pnative` / `-Dnative` 生效）：

- `org.apache.lucene:lucene-core:9.10.0`
- `org.apache.lucene:lucene-analysis-common:9.10.0`

满足删除阶段的类解析即可，不改变产品功能路径。

## 以后怎么避免

- Native 构建出现「可选 / 未使用」第三方的 `NoClassDefFoundError` 时，先查是否有 Quarkus/SVM 的 `@Delete` / `@TargetClass` 仍在解析被删类签名。
- 升级 `quarkus-jdbc-h2` 或 H2 后若再报类似缺类，核对 Lucene 大版本是否仍与 H2 2.3.x 对齐。
