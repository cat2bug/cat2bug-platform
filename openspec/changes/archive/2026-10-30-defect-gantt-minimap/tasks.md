## 1. 偏好与图层入口

- [x] 1.1 扩展 `GANTT_CACHE` / layers 本地结构：增加 `minimap`（缺省 `true`）与面板位置键；读写与现有 `persistGanttLayers` 对齐
- [x] 1.2 图层下拉增加「缩略图」勾选项（divider 与叠加层分隔）；中英文案；与 `showMinimap` 双向绑定

## 2. 浮层与视口映射

- [x] 2.1 在已排期主区挂固定尺寸缩略图面板（默认右下；有记忆则用钳制后的 left/top）；标题区 + 关闭钮
- [x] 2.2 按 `axisBodyScroll` 的 scroll/client/content 尺寸绘制红框，并在 scroll / resize 时更新
- [x] 2.3 拖标题移动面板并写入本地位置；关闭钮隐藏面板并关掉 `minimap` 偏好
- [x] 2.4 拖内容区/红框反算并设置轴 `scrollLeft`/`scrollTop`；保持与左列表纵向同步；`stopPropagation` 避免框选

## 3. 验收与文档

- [x] 3.1 手工验收：默认开、开关/关闭同步、拖标题记位置、拖红框改视口、日/周/月与空数据、不触发框选
- [x] 3.2 回写长期知识：`docs/04` 甘特图层补缩略图一句、`docs/08` 当月一行；确认无需新 ADR

## 4. 缩略图任务色块

- [x] 4.1 按主轴组/行布局生成每行迷你条（含镜像；折叠组跳过）；`barGeometry` 映射到 mini 坐标；状态色；`pointer-events: none`；红框在上
- [x] 4.2 验收：多行/镜像可见、拖色块只导航不选中；docs/04 补「缩略图含状态色块」一句
