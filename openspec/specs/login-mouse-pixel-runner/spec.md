## ADDED Requirements

### Requirement: B2 像素鼠精灵资源

系统 SHALL 使用透明背景像素风精灵图 `mouse_sprite_pixel.png` 作为老鼠唯一/spritesheet 来源。

精灵图 MUST 满足：

- 单帧逻辑尺寸 32×24px 网格
- 含侧面跑、远离、靠近、坐姿与左右看帧行（见 design.md 行语义）
- 绘制时关闭平滑（`imageSmoothingEnabled = false`）

系统 MUST NOT 在验收构建中继续引用 `mouse_sprite_clean.png` 作为主资源。

#### Scenario: 精灵加载后显示像素边缘

- **WHEN** 登录页加载完成且精灵图 onload
- **THEN** 老鼠边缘为硬像素，无模糊双线性缩放

### Requirement: 底部交互区域接收指针事件

老鼠 Canvas 容器 MUST 在页面底部条带（约 120–160px 高）启用 `pointer-events: auto`，以接收鼠标事件。

登录表单输入区 MUST NOT 被老鼠层不可达地遮挡（老鼠层仅覆盖底部条带）。

#### Scenario: 右键在底部条带可按下

- **WHEN** 用户在页面底部条带按下鼠标右键
- **THEN** 老鼠进入用户控制状态，且不弹出浏览器默认上下文菜单

### Requirement: 状态机 — 自动跑（随机左右、出屏）

未按住右键时，老鼠 MUST 处于 `AUTO_RUN` 状态。

每次进入 `AUTO_RUN` 或从屏外重新进入时，水平方向 MUST 以相等概率随机为向左或向右。

老鼠 MUST 能从屏幕一侧外缘进入，向对侧移动，并从另一侧离开屏幕；离开后 MUST 重新生成随机方向并再次从屏外进入。

自动跑时 MAY 伴随尘土粒子效果；粒子颜色 MUST 随明暗主题可读。

#### Scenario: 自动跑出屏后从另一侧再入

- **WHEN** 用户未按右键并观察 ≥1 个完整横穿周期
- **THEN** 老鼠离开可视区域后从对侧再次进入，且方向可能与上一周期不同

### Requirement: 状态机 — 右键按住控制

按住鼠标右键（`button === 2`）时，老鼠 MUST 进入 `USER_CONTROL` 并跟随指针在地面平面移动。

移动时 MUST 根据位移选择精灵行（远离 / 靠近 / 侧面）并更新跑帧动画。

垂直位置 MUST 影响显示尺度（伪 3D：上方更小、下方更大）与移动速度（可选，与 design 一致）。

松开右键后 MUST NOT 立即恢复自动跑。

#### Scenario: 按住右键跟随移动

- **WHEN** 用户在底部条带按住右键并移动指针
- **THEN** 老鼠朝指针方向移动且播放跑步帧

#### Scenario: 松开右键先坐看再自动跑

- **WHEN** 用户松开右键
- **THEN** 老鼠播放坐姿与左右看动画约 2.5–3 秒后再恢复 AUTO_RUN

### Requirement: 伪 3D 与尘土

绘制时 MUST 根据 `mouseY`（相对画布高度）计算 `scale`，使老鼠在条带上方显得更小、下方显得更大。

跑步时 MAY 生成尘土粒子；坐姿/看动画期间 MUST NOT 生成新尘土。

#### Scenario: 靠下更大靠上更小

- **WHEN** 用户在 USER_CONTROL 中将指针移向条带顶部再移向底部
- **THEN** 老鼠显示尺度随垂直位置可辨地变化

### Requirement: 屏外松键与无障碍

在容器外松开右键时，系统 MUST 通过 document 级 `mouseup` 结束 `USER_CONTROL`，避免状态卡死。

当用户启用 `prefers-reduced-motion: reduce` 时，系统 SHOULD 降低或暂停 AUTO_RUN 动画，仍允许表单登录。

#### Scenario: 拖出条带外松键不卡死

- **WHEN** 用户按住右键拖出底部条带后在页面其他区域松键
- **THEN** 老鼠进入 SIT_LOOK 或恢复 AUTO_RUN，而非永久停在 USER_CONTROL
