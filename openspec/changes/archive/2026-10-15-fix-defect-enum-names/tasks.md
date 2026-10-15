## 1. 名字 ↔ 序号转换

- [x] 1.1 `DefectConstants` 新增 `typeName` / `stateName`（序号 → 名字）与 `parseType` / `parseState`（名字或数字 → 序号，空 → null，非法抛异常）
- [x] 1.2 新增 `DefectEnumJson` 的 Jackson 序列化与反序列化器（Quarkus 自动注册反射，Native 已验证）
- [x] 1.3 `SysDefect`：`defectType` 输出名字、输入兼容；`defectState` 输入兼容；新增只读 `defectTypeName`、`defectStateName`，回传时忽略

## 2. 查询参数

- [x] 2.1 `DefectResource.list`、缺陷导出表单、`PlanResource.listDefectOfPlan` 的 `defectType` / `defectState` 改为字符串并解析，非法值返回 400
- [x] 2.2 `DefectResource.list` 支持 `params[defectStates]`；`SysDefectMapper.xml` 按序号列表 `IN` 过滤
- [x] 2.3 Native 下注册 OGNL 反射调用的 JDK 集合类（`MyBatisOgnlReflectionConfig`），修复 `list.size()` 报 `NoSuchMethodException`

## 3. 测试与验收

- [x] 3.1 测试：列表与详情返回名字字段；按名字 / 数字新建；非法类型拒绝；整条回传更新不变；按类型名筛选；状态多选；空筛选忽略
- [x] 3.2 全量测试通过；`build:prod` 不受影响（未改前端）
- [x] 3.3 浏览器验证：缺陷列表状态与类型图标正常、新建缺陷成功、状态多选与类型筛选生效、详情操作按钮出现
- [x] 3.4 Native 重建，两种冒烟通过

## 4. 回写长期知识

- [x] 4.1 `docs/05-接口文档/接口清单-缺陷与用例计划.md` 写明类型 / 状态的输入输出格式与 `params[defectStates]`；`docs/01-产品需求/术语表.md` 核对枚举说明
- [x] 4.2 可复用的坑写入 `docs/06-踩坑记录/`（没有则注明无）；`docs/08-版本记录/` 记录本变更；`check_links.py` 坏链为 0
