## 1. 数据与后端 API

- [x] 1.1 按 ADR-025 新增 Flyway（如 `V1_2_1_3`）：`sys_team.defect_page_config`；实体/`SysTeamMapper` 读写；NULL 视为默认配置
- [x] 1.2 实现配置 DTO/校验（视图至少开一个、绿阈值 &lt; 橙阈值、非负）与默认 JSON
- [x] 1.3 实现 `GET/PUT /system/team/{teamId}/defect-page-settings`（权限复用 `system:team:defect-field:list|edit`，校验可管该团队）
- [x] 1.4 实现项目侧只读 `GET /system/project/{projectId}/defect-page-settings`
- [x] 1.5 分享：`allowShare=false` 时拒 POST 创建 shard，拒匿名 GET，文案「分享已关闭」；补单测

## 2. 团队设置 UI

- [x] 2.1 `DefectSettingsCard` 增加「页面设置」链接；路由 `team-defect-page-settings`（hidden + activeMenu）
- [x] 2.2 实现页面设置表单：四视图开关、存活/驳回阈值与颜色、分享开关；保存走团队 PUT

## 3. 缺陷页生效

- [x] 3.1 缺陷页加载项目页面设置；过滤 radio / 快捷键白名单；缓存非法视图强制降级并写回
- [x] 3.2 表格与详情：存活/驳回**数字**按色阶上色；标签不上色；工具函数可单测
- [x] 3.3 `allowShare=false` 时隐藏 `ShareCard` / 分享入口

## 4. 验证

- [x] 4.1 手测：关看板后按钮消失且缓存降级；改阈值后数字变色；关分享后新建与旧匿名链均失败

## 5. 回写长期知识

- [x] 5.1 更新 `docs/02-系统架构/数据库.md`（`defect_page_config`）
- [x] 5.2 更新术语表 / 接口清单（页面设置字段、分享关闭行为）
- [x] 5.3 `docs/08-版本记录` 当月加一行；无新 ADR；有坑再补踩坑记录
