## ADDED Requirements

### Requirement: 双气泡睡眠符号

猫插画区域 MUST 在猫头附近显示两个睡眠符号：小写 `z`（较小）与大写 `Z`（较大），大 Z 位于小 z 的右上方，与 `mockup_dark_v5.png` 构图一致。

系统 MUST NOT 使用三个独立字母 `Z/z/z` 的旧实现作为最终态。

#### Scenario: 仅显示 z 与 Z 两个符号

- **WHEN** 登录页猫插画可见
- **THEN** 同时存在的睡眠符号为一个小 z 与一个大 Z（非三个循环字母）

### Requirement: 向上飘并淡出动画

两个小气泡 MUST 通过 CSS 动画向上移动（约 32–48px）并降低不透明度至消失，然后循环。

大 Z 的动画启动时间 MUST 晚于小 z（约 0.4–0.6s 延迟），形成连环睡眠感。

动画层 MUST 位于猫场景（Canvas/PNG）之上（`z-index` 高于场景），且 `pointer-events: none`。

#### Scenario: 动画循环可见

- **WHEN** 用户观察猫插画 ≥5 秒
- **THEN** 可见 z 先出现后 Z 跟随上移并淡出，周期重复

### Requirement: 锚点与 GLB 兼容

睡眠符号 MUST 使用绝对定位锚点（`.zzz-anchor`）相对 `.cat-illustration` 放置，在 GLB 未加载（PNG fallback）与 GLB 已加载两种情况下均基本对齐猫头。

实现时允许通过少量 CSS 偏移常量微调，但 MUST NOT 将 zzz 烘焙进 GLB 模型纹理。

#### Scenario: GLB 加载前后均有 zzz

- **WHEN** GLB 加载中显示 fallback PNG
- **THEN** 仍可见睡眠符号动画

- **WHEN** GLB 加载完成显示 3D 场景
- **THEN** 睡眠符号仍可见且位于头顶区域

### Requirement: 主题与减少动效

暗色主题下符号颜色 MUST 为浅灰至白色（`#E5E7EB` 量级），在 `#0D0D0D` 背景上可读。

浅色主题下 MUST 使用足够深的灰色，在浅底上可读。

当 `prefers-reduced-motion: reduce` 时，动画 MUST 停止，符号以静态、半透明形式显示。

#### Scenario: 减少动效时静止显示

- **WHEN** 操作系统开启「减少动态效果」并打开登录页
- **THEN** z 与 Z 静止显示，无向上飘动
