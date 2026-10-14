# 后端拼的 Hash 链接在 History 路由下落到首页

- 日期：2026-10-14
- 涉及：后台 / 前端

## 现象

从 Spring 线移植的推送模版里，缺陷 / 报告 / 通知链接形如 `{host}/#/project/defect?defectId=…`。前端已改为 History 路由，点开这类链接只会进首页（`#` 后面的部分不参与路由），看不到对应缺陷或报告。

## 根因

前端用 `createWebHistory`，项目内页面在 `/{teamKey}/{projectKey}/{module}` 下；旧链接依赖 Hash 路由，且没有团队 / 项目路径键。

## 解决办法

后端统一由 `ImLinkBuilder` 按项目路径键拼链接：缺陷 `/{teamKey}/{projectKey}/defect?projectId=&defectId=`，报告 `/{teamKey}/{projectKey}/report?projectId=&reportId=`，通知 `/notice/index?projectId=`；取不到路径键时用 `project` 作路径段，由前端旧路径重定向兜底。

## 以后怎么避免

- 从 Spring 线（tag `spring-final-1.1.0`）移植任何「后端生成前端链接」的代码（推送、邮件、导出报告里的超链接），都要按 [[项目级History路由与路径键]] 改写，不能照搬 `/#/`。
- 新增此类链接时复用 `ImLinkBuilder`，或至少复用同样的路径键查询。

相关：[[IM多渠道推送]]
