## 1. 后端：项目工作日历

- [x] 1.1 Flyway 建表 `sys_project_work_calendar`（project_id、work_week JSON、exceptions JSON、审计字段）
- [x] 1.2 Domain / Mapper / Service：按 projectId 读默认日历、保存；无行返回默认一～五
- [x] 1.3 REST：GET/PUT 项目日历；导入中国法定按年（读 classpath 资源）；权限：成员可读，创建人/管理员可写
- [x] 1.4 内置 `holidays/cn-YYYY.json`（近年假日+调休）；列出可导入年份接口或静态清单

## 2. 前端：日历编辑与 API

- [x] 2.1 API 封装与 Pinia/组合式加载当前项目日历（甘特与设置共用）
- [x] 2.2 项目设置「工作日历」卡片：工作周、月历点选、例外列表、按年导入（覆盖/仅填空）
- [x] 2.3 缺年提示文案；只读态给非管理员
- [x] 2.4 i18n（日历编辑 + 缺年 + 导入确认）

## 3. 甘特工作日原语

- [x] 3.1 `defect-gantt.js`：`isWorkday` / `nextWorkday` / `addWorkdays` / `countWorkdays`；单测覆盖周末、假日、调休
- [x] 3.2 `planDuration*`、`fsEarliestStartAfterEnd`、落池默认工期改为工作日；注入日历参数
- [x] 3.3 `levelDefectPlans` / `pushOverlapsAfterPinned` 走工作日；支持 `ignoreCalendar`（仅一键路径）
- [x] 3.4 轴 units：假日/调休标记；周末与假日底纹区分；调休不上周末灰

## 4. 甘特 UI 接线

- [x] 4.1 `gantt.vue` 加载项目日历；拖条/改边/落池/避让/自动排列传入日历
- [x] 4.2 自动排列预览增加「忽略日历」勾选（默认关）
- [x] 4.3 图层或工具区「工作日历」入口；天数文案标明工作日
- [x] 4.4 负荷轨只计工作日占用；非工作日不计 overload

## 5. 验证与文档

- [x] 5.1 前端单测：工作日原语、带日历的 level/push 夹具
- [x] 5.2 后端日历 CRUD / 导入冒烟（或集成测）
- [x] 5.3 回写长期知识：`docs/04-UI规范/组件与交互约定.md`、`docs/05` 接口清单、`docs/02-系统架构/数据库.md`、`docs/08-版本记录`；本变更不新增 ADR（日历为排期真相源已落在实现与 OpenSpec，若后续要固化再补）
