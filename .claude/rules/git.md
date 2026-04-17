# Git 工作流规则

## 核心原则

- ✅ **强制使用 worktree 进行功能开发**
- ✅ 每个功能/修复必须在独立分支中进行
- ✅ 创建 worktree 前必须明确源分支
- ✅ 遵循约定式提交规范

## 分支策略

### 分支类型

- **main/master**: 主分支，受保护，仅接受经过审查的合并
- **feature/{功能名}**: 新功能分支
- **fix/{问题描述}**: Bug 修复分支
- **hotfix/{紧急修复}**: 紧急修复分支
- **release/{版本号}**: 发布分支

### 分支命名规范

- 使用小写字母和连字符
- 功能分支: `feature/add-priority-field`
- 修复分支: `fix/login-error`
- 紧急修复: `hotfix/security-patch`
- 发布分支: `release/0.6.2`

**示例:**
- `feature/defect-priority`
- `feature/user-export`
- `fix/null-pointer-exception`
- `fix/login-timeout`
- `hotfix/sql-injection`

## Worktree 工作流（强制）

### 为什么强制使用 Worktree

1. **完全隔离**: 每个功能在独立目录，避免文件冲突和状态混乱
2. **并行开发**: 可同时处理多个功能，无需频繁切换分支
3. **状态保持**: 每个 worktree 保持独立的工作状态（未提交的修改、构建产物等）
4. **减少错误**: 避免在错误分支上提交代码
5. **提高效率**: 无需等待构建完成即可切换任务

### 创建 Worktree（必须询问源分支）

#### 步骤 1: 确定源分支（必须）

在创建 worktree 前，**必须询问用户**：
- **从哪个分支创建新分支？**

常见选择：
- `main` 或 `master`: 新功能开发（最常见）
- `develop`: 如果项目使用 Git Flow 工作流
- 其他功能分支: 基于现有功能进行扩展
- `release/x.x.x`: 发布分支的 Bug 修复

**示例对话:**
```
Claude: 我将为您创建一个新的功能分支。请问您希望从哪个分支创建？
- main (推荐，用于新功能)
- develop
- 其他分支（请指定）

User: main

Claude: 好的，我将从 main 分支创建新的 worktree。
```

#### 步骤 2: 创建 worktree

**重要：所有 worktree 必须创建在项目的 `.worktree` 目录下**

```bash
# 从 main 分支创建新功能分支（推荐方式）
git worktree add -b feature/new-feature .worktree/feature-new-feature main

# 从 develop 分支创建
git worktree add -b feature/new-feature .worktree/feature-new-feature develop

# 从现有分支创建
git worktree add -b fix/bug-123 .worktree/fix-bug-123 feature/existing-feature

# 从远程分支创建（如果分支已存在）
git worktree add .worktree/feature-existing origin/feature/existing
```

#### 步骤 3: 复制项目文档和规则到 worktree（必须）

**创建 worktree 后，必须将项目根目录的 CLAUDE.md 和 .claude 文件夹复制到新的 worktree 目录中**

```bash
# 假设刚创建了 .worktree/feature-new-feature
# 复制 CLAUDE.md
cp CLAUDE.md .worktree/feature-new-feature/

# 复制 .claude 文件夹及其所有内容
cp -r .claude .worktree/feature-new-feature/
```

**为什么需要复制？**
- 确保每个 worktree 都有完整的项目文档和开发规范
- 允许在不同分支中独立维护和更新文档
- 保证 Claude Code 在任何 worktree 中都能访问到项目规则

**自动化脚本（推荐）：**
```bash
# 创建一个辅助函数，自动创建 worktree 并复制文档
create_worktree() {
    local branch_name=$1
    local worktree_name=$2
    local source_branch=${3:-main}

    # 创建 worktree
    git worktree add -b "$branch_name" ".worktree/$worktree_name" "$source_branch"

    # 复制文档和规则
    cp CLAUDE.md ".worktree/$worktree_name/"
    cp -r .claude ".worktree/$worktree_name/"

    echo "✅ Worktree created and documentation copied to .worktree/$worktree_name"
}

# 使用示例
create_worktree feature/new-feature feature-new-feature main
```

**为什么使用 `.worktree` 目录？**
1. **集中管理**: 所有 worktree 在一个位置，便于查找和管理
2. **避免混乱**: 不会在父目录创建大量分散的目录
3. **易于清理**: 可以一次性清理所有 worktree
4. **Git 忽略**: `.worktree` 目录已在 `.gitignore` 中，不会被提交

#### 目录结构（新规范）

```
cat2bug-platform/                  # 主仓库（main 分支）
├── .git/                          # Git 仓库数据
├── .worktree/                     # 所有 worktree 存放目录
│   ├── feature-priority/          # 功能分支 worktree
│   ├── fix-login-error/           # 修复分支 worktree
│   └── hotfix-security/           # 紧急修复 worktree
├── src/                           # 源代码
└── ...
```

#### 旧目录结构（不推荐）

```
/Users/yuzhantao/projects/git/cat2bug/
├── cat2bug-platform/              # 主仓库（main 分支）
├── cat2bug-feature-priority/      # ❌ 不推荐：在父目录创建
├── cat2bug-fix-login-error/       # ❌ 不推荐：在父目录创建
└── cat2bug-hotfix-security/       # ❌ 不推荐：在父目录创建
```

#### Worktree 目录命名规范

格式: `{项目名}-{分支类型}-{功能名}`

**示例:**
- `cat2bug-feature-priority`
- `cat2bug-feature-user-export`
- `cat2bug-fix-login`
- `cat2bug-hotfix-security`

### 使用 Worktree

#### 开发流程

1. **进入 worktree 目录**
   ```bash
   cd ../cat2bug-feature-new-feature
   ```

2. **确认当前分支**
   ```bash
   git branch --show-current
   # 输出: feature/new-feature
   ```

3. **正常开发、提交**
   ```bash
   # 查看状态
   git status

   # 添加文件
   git add .

   # 提交（遵循约定式提交规范）
   git commit -m "feat: add new feature"
   ```

4. **推送到远程**
   ```bash
   # 首次推送
   git push -u origin feature/new-feature

   # 后续推送
   git push
   ```

#### 提交规范（约定式提交）

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

**格式:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型（type）:**
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具相关
- `perf`: 性能优化

**示例:**
```bash
# 简单提交
git commit -m "feat: add defect priority field"
git commit -m "fix: resolve null pointer in login"
git commit -m "docs: update API documentation"

# 带作用域
git commit -m "feat(defect): add priority filter"
git commit -m "fix(auth): handle token expiration"

# 带详细说明
git commit -m "feat(defect): add priority field

- Add priority column to sys_defect table
- Add priority filter in defect list
- Add priority selector in defect form

Closes #123"
```

### 合并流程

1. **确保代码已提交并推送**
   ```bash
   git status  # 确保工作区干净
   git push    # 推送到远程
   ```

2. **创建 Pull Request / Merge Request**
   - 在 GitHub/GitLab/Gitee 上创建 PR/MR
   - 填写清晰的描述
   - 关联相关 Issue

3. **代码审查**
   - 等待团队成员审查
   - 根据反馈修改代码
   - 确保 CI/CD 通过

4. **合并**
   - 审查通过后合并到目标分支
   - 删除远程分支（可选）

5. **清理 worktree**（见下节）

### 清理 Worktree

#### 查看所有 worktree

```bash
git worktree list
```

输出示例:
```
/Users/yuzhantao/projects/git/cat2bug/cat2bug-platform              abc1234 [main]
/Users/yuzhantao/projects/git/cat2bug/cat2bug-feature-priority     def5678 [feature/priority]
/Users/yuzhantao/projects/git/cat2bug/cat2bug-fix-login            ghi9012 [fix/login]
```

#### 删除 worktree

**方法 1: 使用 remove 命令（推荐）**
```bash
git worktree remove ../cat2bug-feature-new-feature
```

**方法 2: 手动删除目录 + 清理引用**
```bash
# 删除目录
rm -rf ../cat2bug-feature-new-feature

# 清理引用
git worktree prune
```

#### 清理本地分支

```bash
# 删除已合并的本地分支
git branch -d feature/new-feature

# 强制删除未合并的分支（谨慎使用）
git branch -D feature/new-feature
```

#### 完整清理流程

```bash
# 1. 确认分支已合并
git branch --merged main

# 2. 删除 worktree
git worktree remove ../cat2bug-feature-new-feature

# 3. 删除本地分支
git branch -d feature/new-feature

# 4. 删除远程分支（可选）
git push origin --delete feature/new-feature

# 5. 清理无效引用
git worktree prune
```

## 常见场景

### 场景 1: 开始新功能

```bash
# 1. 询问用户从哪个分支创建（假设回答: main）
# 2. 创建 worktree
git worktree add -b feature/user-export ../cat2bug-feature-user-export main

# 3. 进入目录开发
cd ../cat2bug-feature-user-export

# 4. 开发...
# 5. 提交
git add .
git commit -m "feat: add user export functionality"

# 6. 推送
git push -u origin feature/user-export
```

### 场景 2: 修复 Bug

```bash
# 1. 询问用户从哪个分支创建（假设回答: main）
# 2. 创建 worktree
git worktree add -b fix/login-timeout ../cat2bug-fix-login-timeout main

# 3. 进入目录修复
cd ../cat2bug-fix-login-timeout

# 4. 修复...
# 5. 提交
git add .
git commit -m "fix: resolve login timeout issue"

# 6. 推送
git push -u origin fix/login-timeout
```

### 场景 3: 紧急修复

```bash
# 1. 从 main 创建 hotfix
git worktree add -b hotfix/security-patch ../cat2bug-hotfix-security main

# 2. 进入目录修复
cd ../cat2bug-hotfix-security

# 3. 修复代码
# 4. 提交
git add .
git commit -m "fix: patch SQL injection vulnerability"

# 5. 推送
git push -u origin hotfix/security-patch

# 6. 创建 PR 并快速合并
# 7. 合并后立即清理
cd ../cat2bug-platform
git worktree remove ../cat2bug-hotfix-security
git branch -d hotfix/security-patch
```

### 场景 4: 基于现有功能开发

```bash
# 1. 询问用户从哪个分支创建（假设回答: feature/base-feature）
# 2. 创建 worktree
git worktree add -b feature/extended-feature ../cat2bug-feature-extended feature/base-feature

# 3. 进入目录开发
cd ../cat2bug-feature-extended

# 4. 开发...
```

### 场景 5: 同时处理多个任务

```bash
# 任务 1: 新功能
git worktree add -b feature/task1 ../cat2bug-feature-task1 main

# 任务 2: Bug 修复
git worktree add -b fix/task2 ../cat2bug-fix-task2 main

# 在任务 1 和任务 2 之间自由切换
cd ../cat2bug-feature-task1  # 工作在任务 1
cd ../cat2bug-fix-task2       # 切换到任务 2
```

## 常见问题

### Q: 忘记从哪个分支创建了？

```bash
# 方法 1: 查看分支的上游分支
git branch -vv

# 方法 2: 查看分支历史
git log --oneline --graph --all --decorate

# 方法 3: 查看分支的第一个提交
git log --reverse --oneline
```

### Q: Worktree 目录被误删？

```bash
# 1. 清理无效引用
git worktree prune

# 2. 如果分支还在，重新创建 worktree
git worktree add ../cat2bug-feature-xxx feature/xxx

# 3. 如果分支在远程，从远程创建
git worktree add ../cat2bug-feature-xxx origin/feature/xxx
```

### Q: 需要在多个 worktree 间共享修改？

**方法 1: 使用 patch**
```bash
# 在源 worktree 中创建补丁
git format-patch -1 HEAD

# 在目标 worktree 中应用
git am < 0001-xxx.patch
```

**方法 2: 使用 cherry-pick**
```bash
# 在源 worktree 中查看提交 hash
git log --oneline -1

# 在目标 worktree 中 cherry-pick
git cherry-pick <commit-hash>
```

**方法 3: 使用 stash（仅限未提交的修改）**
```bash
# 在源 worktree 中
git stash

# 在目标 worktree 中
git stash pop
```

### Q: Worktree 占用太多磁盘空间？

- ✅ 及时清理已合并的 worktree
- ✅ 使用 `git worktree prune` 清理无效引用
- ✅ 定期运行 `git gc` 清理仓库
- ✅ 删除不再需要的本地分支

```bash
# 清理无效引用
git worktree prune

# 清理仓库
git gc --aggressive --prune=now

# 查看磁盘占用
du -sh .git
```

### Q: 如何在 worktree 中运行构建？

每个 worktree 都是独立的工作目录，可以独立构建：

```bash
# 后端构建
cd ../cat2bug-feature-xxx
mvn clean package

# 前端构建
cd cat2bug-platform-ui
npm run build:prod
```

### Q: Worktree 中的配置文件会冲突吗？

- `.git/config` 是共享的（在主仓库中）
- 工作区的配置文件（如 `application.yml`）是独立的
- 建议使用环境变量或 profile 区分不同 worktree 的配置

## 禁止事项

- ❌ **禁止在主仓库目录直接切换分支进行功能开发**
- ❌ **禁止在未询问源分支的情况下创建 worktree**
- ❌ 禁止在 worktree 中修改 `.git/config`（应在主仓库修改）
- ❌ 禁止多个 worktree 使用相同分支
- ❌ 禁止在 worktree 中执行 `git worktree add`（应在主仓库执行）
- ❌ 禁止提交包含敏感信息的代码（密钥、密码等）
- ❌ 禁止使用 `git push --force` 到共享分支

## 审查清单

### 开始开发前

- [ ] 已询问并确认源分支
- [ ] 已创建独立的 worktree
- [ ] Worktree 目录命名符合规范
- [ ] 已确认当前分支正确
- [ ] 已拉取最新代码（`git pull`）

### 提交代码前

- [ ] Commit message 符合约定式提交规范
- [ ] 代码已通过本地测试（`mvn test`）
- [ ] 代码已通过代码检查（`npm run lint`）
- [ ] 无敏感信息（密钥、密码等）
- [ ] 已推送到远程分支

### 合并后

- [ ] 已删除 worktree 目录
- [ ] 已删除本地分支
- [ ] 已运行 `git worktree prune`
- [ ] 已删除远程分支（可选）

## 相关命令速查

```bash
# 查看所有 worktree
git worktree list

# 创建 worktree
git worktree add -b <branch> <path> <source-branch>

# 删除 worktree
git worktree remove <path>

# 清理无效引用
git worktree prune

# 查看当前分支
git branch --show-current

# 查看所有分支
git branch -a

# 删除本地分支
git branch -d <branch>

# 删除远程分支
git push origin --delete <branch>

# 清理仓库
git gc --prune=now
```

## 参考资料

- [Git Worktree 官方文档](https://git-scm.com/docs/git-worktree)
- [Conventional Commits 规范](https://www.conventionalcommits.org/)
- [Git Flow 工作流](https://nvie.com/posts/a-successful-git-branching-model/)
