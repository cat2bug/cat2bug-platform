# 改目录名后 .gitignore 旧规则失效导致敏感配置暴露

模块目录改名时，含密码的本机配置差点被提交的原因。

- 日期：2026-10-14
- 涉及：后台 / 部署

## 现象

把 `cat2bug-platform-quarkus-admin` `git mv` 成 `cat2bug-platform-admin` 之后，`git status` 里突然出现未跟踪的 `cat2bug-platform-admin/config/install/`，里面是安装向导写入的 `application-install.yml`、`quarkus-runtime.properties`，含数据库密码。此时只要执行 `git add -A` 就会把密码提交进库。

## 根因

`.gitignore` 里的忽略规则写的是带目录名的绝对路径（`/cat2bug-platform-quarkus-admin/config/install/`）。目录改名后旧规则不再匹配，新路径又没有规则，本机这些被忽略的文件就变成了未跟踪文件。`git mv` 只移动已跟踪文件，被忽略的本机文件随目录一起移动，但不会自动获得新的忽略规则。

## 解决办法

在同一个改名提交里把 `.gitignore` 中的旧目录名全部改为新目录名（如 `/cat2bug-platform-admin/config/install/`、`/cat2bug-platform-admin/upload/`、`/cat2bug-platform-admin/src/main/resources/META-INF/resources/`），确认 `git status` 不再出现敏感文件后再提交（提交 `82440389`）。

## 以后怎么避免

- 改目录名**之前**先改 `.gitignore`：`rg -n '旧目录名' .gitignore`，把规则改成新名字（必要时新旧并存），再 `git mv`。
- 改名后、提交前跑 `git status --ignored` 和 `git status`，确认 `config/install`、`upload`、`data` 之类目录仍处于忽略状态。
- 不要在改名类大提交里用 `git add -A` 一把梭，按路径添加。
