# Native 冒烟默认模式缺少数据源时复用空库导致项目检查失败

Native 冒烟脚本里「缺陷导入模板 500」「用例导入为空」不一定是 Native 回归。

- 日期：2026-10-14
- 涉及：部署 / 测试

## 现象

`./cat2bug-platform-admin/smoke-native-docker.sh arm64`（默认模式）只有两项失败：

- `defect import template: FAIL (HTTP 500)`，接口返回 `当前项目不能为空`
- `case excel import: FAIL`，返回「导入用例数据为空或格式不合法」

## 根因

默认模式从 `/tmp/cat2bug-smoke/`（`NATIVE_SMOKE_INSTALL` / `NATIVE_SMOKE_RUNTIME`）复制一份**有业务数据的 H2 安装**进容器。这个目录不在时，脚本不会报错，而是直接复用 `/tmp/cat2bug-native-smoke` 里上一次 `FRESH=true` 留下的空库：

- 缺陷导入模板按**用户配置里的当前项目**（`sys_user_config.current_project_id`）取模块列表，与 Spring 线一致；空库的 admin 没进过项目，当前项目为空。
- 用例导入用的是刚导出的用例 Excel，项目里没有用例，导出的就是空表。

Native 二进制只支持 H2（db-kind 是构建期属性，见 [[Quarkus数据源db-kind是构建期属性]]），不能直接拿 MySQL 开发库当数据源。

## 解决办法

先跑 `FRESH=true KEEP_CONTAINER=true`，在保留的容器里用接口准备数据，再跑默认模式：

1. `POST /system/team`（`teamName`、`teamKey`）
2. `POST /system/project`（`projectName`、`projectKey`、`teamId`）
3. `POST /system/case` 建一条用例
4. `PUT /system/user-config`，body `{"currentProjectId":<项目ID>}`

## 以后怎么避免

- 默认模式找不到 `/tmp/cat2bug-smoke/` 时，看到项目相关检查失败先确认数据来源，不要直接按 Native 回归排查。
- 改临时目录名（如 `cat2bug-quarkus-smoke` → `cat2bug-smoke`）时，同步迁移本机已有的冒烟数据，见 [[部署]]。
