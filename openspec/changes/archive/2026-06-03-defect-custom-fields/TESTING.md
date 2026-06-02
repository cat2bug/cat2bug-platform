# 缺陷自定义字段 — 测试说明

## 前置

- 使用具备 `system:project:option` 权限的账号
- 准备测试项目 A，至少 2 条缺陷
- 分别验证 **H2** 开发环境与 **MySQL**（若可）各一组 Tab 筛选

## 1. 字段定义 CRUD

- [ ] 项目设置 → 缺陷设置 → 自定义字段：新增 `env`（string，必填）、`severity`（enum，P0/P1 带颜色）
- [ ] 编辑 label、sort_order 生效；尝试改 `field_key` 应失败
- [ ] 软删除 `env` 后列表不再出现；重新启用后恢复
- [ ] 第 51 个启用字段应被拒绝

## 2. 新增 / 编辑 / 查看

- [ ] 新增缺陷：填写 `env`、`severity`，保存成功
- [ ] 缺填必填 `env` 应前端+服务端拦截
- [ ] 编辑缺陷：枚举回显正确，修改后列表与详情一致
- [ ] 查看抽屉：枚举色标、字符串正常；对象/数组/图片/文件类型各测 1 条（若已配置）

## 3. 列表与列显隐

- [ ] 列设置中出现 `custom:env` 等，隐藏后表格不显示
- [ ] 刷新页面后隐藏状态保持（本地缓存）
- [ ] 表格单元格格式与详情一致（枚举为 label）

## 4. Excel

- [ ] Excel 视图出现自定义列，列显隐与表格同步
- [ ] 导出文件含自定义列且枚举为 **label**
- [ ] 导入：合法行写入；非法枚举行报错；必填空行报错

## 5. Tab 筛选（MVP）

- [ ] 新建 Tab：`severity in P0`，激活后列表仅 P0
- [ ] 切换到「全部」Tab：无上一 Tab 筛选残留
- [ ] 字符串 `env contains prod` 生效
- [ ] 文件/对象字段 `isNotEmpty` / `isEmpty` 各测 1 次

## 6. 回归

- [ ] 无自定义字段的项目：缺陷页无空白占位异常
- [ ] 固定字段（类型、状态、参与筛选）行为与变更前一致
- [ ] `searchQuery` + 我的参与热力图：不受 `customFieldFilters` 误伤

## 7. 自动化（实现后勾选）

- [ ] `SysProjectDefectFieldService` 单测通过
- [ ] `custom_fields` 校验单测通过
- [ ] H2 + MySQL `customFieldFilters` 集成测试通过
