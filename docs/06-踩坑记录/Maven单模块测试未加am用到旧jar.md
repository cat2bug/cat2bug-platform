# Maven 单模块测试未加 -am 用到旧 jar

记录「源码已改、测试结果却像没改」的常见原因。

- 日期：2026-10-14（待确认：首次发生日期未记录）
- 涉及：Quarkus 后端 / Spring 后端

## 现象

只跑某个模块的测试（如 `mvn -Pquarkus -pl cat2bug-platform-quarkus-admin test`），依赖模块（framework / system / common）里刚改的代码没有生效，测试行为与源码不符。

## 根因

`-pl` 只构建指定模块，依赖模块从本地仓库 `~/.m2` 取之前 `install` 过的旧 jar，而不是用当前源码重新编译。

## 解决办法

单模块测试时加 `-am`（also-make），让 Maven 一并构建上游依赖模块：

```bash
mvn -pl cat2bug-platform-admin -am test
```

（2026-10-14 模块改名后（见 [[ADR-010-移除Spring后端并统一模块命名]]）不再需要 `-Pquarkus`，模块名也去掉了 `quarkus`；当时的写法是 `mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am test`。）

只想跑某个测试时可配合 `-Dtest=... -Dsurefire.failIfNoSpecifiedTests=false`（待确认：项目内是否已统一此写法）。

## 以后怎么避免

- 跨模块改动后，`-pl` 一律带 `-am`；`CLAUDE.md` 中的构建命令已统一带 `-am`。
- 测试结果与源码对不上时，先怀疑用到了本地仓库旧 jar。
