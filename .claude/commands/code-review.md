# /code-review - 代码审查命令

## 用途
对未提交的代码改动进行安全和质量审查，按严重程度分类问题。

## 审查范围
- 未提交的改动（git diff）
- 未暂存的改动（git diff HEAD）

## 审查维度

### 1. 安全审查（最高优先级）
- 硬编码密钥、Token、密码
- SQL 注入风险
- XSS 风险
- 命令注入风险
- 路径遍历漏洞
- 未授权访问
- 敏感信息泄露

### 2. 质量审查
- 代码重复
- 方法过长
- 类过大
- 循环复杂度
- 错误处理
- 资源泄露

### 3. 测试审查
- 缺少测试
- 测试覆盖不足
- 测试质量问题

### 4. 文档审查
- 缺少文档
- 文档过时
- 注释不足

## 严重程度分级

### 🔴 CRITICAL（阻断级）
- 硬编码密钥
- SQL 注入
- 命令注入
- 未授权访问
- 修改核心逻辑但未补充测试
- 现有测试失败

### 🟠 HIGH（高危）
- XSS 风险
- 文件上传漏洞
- 弱加密算法
- 缺少输入校验
- 新增 API 但无测试
- 代码重复严重

### 🟡 MEDIUM（中危）
- 日志记录敏感信息
- 错误信息暴露细节
- 缺少速率限制
- 方法过长
- 缺少注释

### 🟢 LOW（低优先级）
- 命名不规范
- 可优化的性能
- 注释拼写错误
- 未使用的导入

## 执行流程

### 1. 获取改动
```bash
# 查看未提交的改动
git diff

# 查看所有改动（包括已暂存）
git diff HEAD

# 查看改动统计
git diff --stat
```

### 2. 逐文件审查
对每个改动的文件进行审查：
- 安全问题
- 质量问题
- 测试覆盖
- 文档完整性

### 3. 生成报告
按严重程度分类问题，提供修复建议。

## 输出格式

```markdown
# 代码审查报告

## 概览
- 审查时间：{时间}
- 改动文件：{数量} 个
- 新增代码：+{行数} 行
- 删除代码：-{行数} 行

---

## 🔴 CRITICAL 问题（必须修复）

{如果没有，显示：✅ 无 CRITICAL 问题}

### 问题 1：{问题标题}
- **文件**：`{文件路径}:{行号}`
- **类型**：安全/质量/测试/文档
- **描述**：{详细描述}
- **风险**：{风险说明}
- **建议**：{修复建议}

```{language}
// ❌ 错误做法
{错误代码}

// ✅ 正确做法
{正确代码}
```

---

## 🟠 HIGH 问题（强烈建议修复）

{如果没有，显示：✅ 无 HIGH 问题}

---

## 🟡 MEDIUM 问题（建议修复）

{如果没有，显示：✅ 无 MEDIUM 问题}

---

## 🟢 LOW 问题（可选修复）

{如果没有，显示：✅ 无 LOW 问题}

---

## ✅ 良好实践

{列出代码中的良好实践}

---

## 总结
- **阻断合并**：{是/否}
- **CRITICAL 问题**：{数量}
- **HIGH 问题**：{数量}
- **MEDIUM 问题**：{数量}
- **LOW 问题**：{数量}
- **建议**：{总体建议}
```

## 阻断规则

以下情况必须阻断合并：
- 存在 CRITICAL 级别的安全问题
- 存在 CRITICAL 级别的测试问题
- 存在硬编码密钥
- 存在 SQL 注入风险
- 存在未授权访问风险
- 现有测试失败

## 审查清单

### 安全审查清单
- [ ] 无硬编码密钥、Token、密码
- [ ] 无 SQL 注入风险（使用 #{} 而非 ${}）
- [ ] 无 XSS 风险（输出已转义）
- [ ] 无命令注入风险
- [ ] 无路径遍历漏洞
- [ ] 权限检查完整（@PreAuthorize）
- [ ] 输入已校验（@Valid）
- [ ] 敏感信息已脱敏

### 质量审查清单
- [ ] 无重复代码
- [ ] 方法长度合理（< 50 行）
- [ ] 类大小合理（< 500 行）
- [ ] 循环复杂度合理
- [ ] 错误处理完善
- [ ] 资源已释放（try-with-resources）
- [ ] 命名清晰规范
- [ ] 有必要的注释

### 测试审查清单
- [ ] 新增功能有测试
- [ ] 修改逻辑有测试
- [ ] 测试覆盖正常和异常场景
- [ ] 测试可独立运行
- [ ] 所有测试通过

### 文档审查清单
- [ ] 新增 API 有文档
- [ ] 复杂逻辑有注释
- [ ] 公共方法有 Javadoc
- [ ] 配置变更有说明
- [ ] 破坏性变更有记录

## 示例

### 示例 1：发现硬编码密钥

```markdown
## 🔴 CRITICAL 问题 1：硬编码数据库密码

- **文件**：`application.yml:15`
- **类型**：安全
- **描述**：数据库密码直接写在配置文件中
- **风险**：密码泄露，数据库被非法访问
- **建议**：
  1. 将密码移到环境变量
  2. 在 .gitignore 中排除包含密码的配置文件
  3. 提供 .example 模板文件

```yaml
# ❌ 错误做法
spring:
  datasource:
    password: admin123

# ✅ 正确做法
spring:
  datasource:
    password: ${DB_PASSWORD}
```
```

### 示例 2：缺少输入校验

```markdown
## 🟠 HIGH 问题 1：缺少输入校验

- **文件**：`DefectController.java:45`
- **类型**：安全
- **描述**：createDefect 方法未校验输入参数
- **风险**：可能导致非法数据入库或 SQL 注入
- **建议**：在 Controller 方法参数上添加 @Valid 注解

```java
// ❌ 错误做法
@PostMapping
public AjaxResult add(SysDefect defect) {
    return toAjax(defectService.insertDefect(defect));
}

// ✅ 正确做法
@PostMapping
public AjaxResult add(@Valid @RequestBody SysDefect defect) {
    return toAjax(defectService.insertDefect(defect));
}
```
```

### 示例 3：缺少测试

```markdown
## 🔴 CRITICAL 问题 1：修改核心逻辑但未补充测试

- **文件**：`DefectService.java:78`
- **类型**：测试
- **描述**：修改了 updateDefectStatus 方法的状态流转逻辑，但未补充测试
- **风险**：可能引入 Bug，影响现有功能
- **建议**：补充单元测试，覆盖所有状态流转场景

```java
@Test
public void testUpdateDefectStatus_fromNewToInProgress() {
    // Given
    SysDefect defect = new SysDefect();
    defect.setDefectId(1L);
    defect.setDefectState("NEW");

    // When
    defectService.updateDefectStatus(1L, "IN_PROGRESS");

    // Then
    SysDefect updated = defectService.selectDefectById(1L);
    assertEquals("IN_PROGRESS", updated.getDefectState());
}
```
```

## 注意事项

- ✅ 按严重程度分类问题
- ✅ 提供具体的修复建议和代码示例
- ✅ 指出代码中的良好实践
- ✅ 明确是否阻断合并
- ❌ 不要过于苛刻（LOW 问题不应阻断）
- ❌ 不要只指出问题不给建议
- ❌ 不要忽略安全问题

## 相关命令
- `/plan` - 制定实施计划
- `/tdd` - 使用 TDD 方式开发
- `/build-fix` - 修复构建错误
