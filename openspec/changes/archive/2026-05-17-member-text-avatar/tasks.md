## 1. 工具函数

- [x] 1.1 新建 `cat2bug-platform-ui/src/utils/member-avatar.js`：`getAvatarInitial`、`getAvatarColorKey`、`getAvatarPaletteStyle`（固定色板 + 白字 + `fontWeight: 700`）、`resolveMemberDisplayName`
- [x] 1.2 实现首字符哈希选色（`hashString(colorKey) % AVATAR_PALETTE.length`），不引入拼音库
- [x] 1.3 导出 `AVATAR_PALETTE` 常量供文档与测试引用

## 2. Cat2BugAvatar

- [x] 2.1 无图时 slot 仅显示 `getAvatarInitial(displayName)` 单字符
- [x] 2.2 无图时 `el-avatar` 应用 `getAvatarPaletteStyle` 内联样式
- [x] 2.3 有图时逻辑不变；`isStatistics` 保持灰色样式
- [x] 2.4 增加 `.cat2bug-avatar--text-initial` 加粗样式；支持 `avatar` 为绝对 URL

## 3. 调用方统一

- [x] 3.1 确认 `RowListMember`、`SelectProjectMember` 等经 `Cat2BugAvatar` 的展示符合 spec
- [x] 3.2 `MemberNameplate` 改用 `<cat2-bug-avatar>`（指派等下拉）
- [x] 3.3 `AddProjectMember.vue` 成员选项改用 `<cat2-bug-avatar>`
- [x] 3.4 `InviteTeamMember.vue` 成员选项改用 `<cat2-bug-avatar>`

## 4. 验证

- [x] 4.1 无头像成员：中文首字、英文首字母大写、加粗白字、色板背景
- [x] 4.2 不同首字成员背景色可区分；同首字稳定同色
- [x] 4.3 有头像成员仍显示图片
