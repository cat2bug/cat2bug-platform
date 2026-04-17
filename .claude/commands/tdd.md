# /tdd - 测试驱动开发命令

## 用途
使用 TDD（Test-Driven Development）方式实现功能，确保代码质量和测试覆盖。

## TDD 三步法

### RED - 先写失败的测试
### GREEN - 实现最小可用代码
### REFACTOR - 重构优化

## 执行流程

### 1. RED - 写失败的测试

#### 步骤
1. 根据需求编写测试用例
2. 测试必须失败（因为功能还未实现）
3. 运行测试，确认失败

#### 示例
```java
@Test
public void testCreateDefect() {
    // Given - 准备测试数据
    SysDefect defect = new SysDefect();
    defect.setDefectName("测试缺陷");
    defect.setDefectType("BUG");
    defect.setDefectState("NEW");

    // When - 执行被测试方法
    int result = defectService.insertDefect(defect);

    // Then - 验证结果
    assertEquals(1, result);
    assertNotNull(defect.getDefectId());
    assertEquals("NEW", defect.getDefectState());
}
```

#### 运行测试
```bash
mvn test -Dtest=DefectServiceTest#testCreateDefect
```

#### 预期结果
```
❌ Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
```

### 2. GREEN - 实现最小可用代码

#### 原则
- 只写让测试通过的代码
- 不过度设计
- 不添加未测试的功能

#### 示例
```java
@Override
public int insertDefect(SysDefect defect) {
    // 设置创建时间和创建人
    defect.setCreateTime(DateUtils.getNowDate());
    defect.setCreateBy(SecurityUtils.getUsername());

    // 如果未设置状态，默认为 NEW
    if (StringUtils.isEmpty(defect.getDefectState())) {
        defect.setDefectState("NEW");
    }

    // 插入数据库
    return defectMapper.insertDefect(defect);
}
```

#### 运行测试
```bash
mvn test -Dtest=DefectServiceTest#testCreateDefect
```

#### 预期结果
```
✅ Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

### 3. REFACTOR - 重构优化

#### 原则
- 消除重复代码
- 提取公共方法
- 优化命名
- 简化逻辑
- 保持测试通过

#### 示例
```java
// 重构前
@Override
public int insertDefect(SysDefect defect) {
    defect.setCreateTime(DateUtils.getNowDate());
    defect.setCreateBy(SecurityUtils.getUsername());
    if (StringUtils.isEmpty(defect.getDefectState())) {
        defect.setDefectState("NEW");
    }
    return defectMapper.insertDefect(defect);
}

// 重构后
@Override
public int insertDefect(SysDefect defect) {
    prepareDefectForInsert(defect);
    return defectMapper.insertDefect(defect);
}

private void prepareDefectForInsert(SysDefect defect) {
    setAuditFields(defect);
    setDefaultState(defect);
}

private void setAuditFields(SysDefect defect) {
    defect.setCreateTime(DateUtils.getNowDate());
    defect.setCreateBy(SecurityUtils.getUsername());
}

private void setDefaultState(SysDefect defect) {
    if (StringUtils.isEmpty(defect.getDefectState())) {
        defect.setDefectState(DefectState.NEW.getCode());
    }
}
```

#### 运行测试
```bash
mvn test -Dtest=DefectServiceTest
```

#### 预期结果
```
✅ Tests run: X, Failures: 0, Errors: 0, Skipped: 0
```

## 完整 TDD 循环

```
1. RED   → 写测试 → 运行测试（失败）
2. GREEN → 写代码 → 运行测试（通过）
3. REFACTOR → 重构 → 运行测试（通过）
4. 重复 1-3，直到功能完成
```

## 测试用例设计

### 测试场景
- ✅ 正常场景（Happy Path）
- ✅ 边界场景（Boundary）
- ✅ 异常场景（Exception）
- ✅ 空值场景（Null/Empty）

### 示例
```java
public class DefectServiceTest {

    @Autowired
    private IDefectService defectService;

    // 正常场景
    @Test
    public void testCreateDefect_success() {
        SysDefect defect = createValidDefect();
        int result = defectService.insertDefect(defect);
        assertEquals(1, result);
    }

    // 边界场景
    @Test
    public void testCreateDefect_withMaxLengthName() {
        SysDefect defect = createValidDefect();
        defect.setDefectName(StringUtils.repeat("a", 200)); // 最大长度
        int result = defectService.insertDefect(defect);
        assertEquals(1, result);
    }

    // 异常场景
    @Test(expected = BusinessException.class)
    public void testCreateDefect_withNullName() {
        SysDefect defect = createValidDefect();
        defect.setDefectName(null);
        defectService.insertDefect(defect);
    }

    // 空值场景
    @Test
    public void testCreateDefect_withEmptyDescription() {
        SysDefect defect = createValidDefect();
        defect.setDefectDescription("");
        int result = defectService.insertDefect(defect);
        assertEquals(1, result);
    }

    private SysDefect createValidDefect() {
        SysDefect defect = new SysDefect();
        defect.setDefectName("测试缺陷");
        defect.setDefectType("BUG");
        return defect;
    }
}
```

## 约束规则

### 必须遵守
- ✅ 必须先写测试，再写实现
- ✅ 测试必须先失败，再通过
- ✅ 每次只实现一个测试用例
- ✅ 重构后必须运行测试
- ❌ 禁止跳过 RED 阶段直接写实现
- ❌ 禁止写未测试的代码
- ❌ 禁止在测试未通过时重构

### 测试质量
- 测试必须独立（不依赖其他测试）
- 测试必须可重复（每次运行结果一致）
- 测试必须快速（单个测试 < 1 秒）
- 测试必须清晰（Given-When-Then 结构）

## 命令使用

### 基本用法
```
/tdd 实现缺陷创建功能
```

### 指定测试类
```
/tdd 实现缺陷创建功能 --test-class=DefectServiceTest
```

### 指定测试方法
```
/tdd 实现缺陷创建功能 --test-method=testCreateDefect
```

## 输出格式

```markdown
# TDD 实施：{功能名称}

## 当前循环：{RED/GREEN/REFACTOR}

### RED - 编写测试

**测试类：** `{测试类路径}`
**测试方法：** `{测试方法名}`

```java
{测试代码}
```

**运行测试：**
```bash
mvn test -Dtest={测试类}#{测试方法}
```

**预期结果：** ❌ 测试失败

---

### GREEN - 实现代码

**实现类：** `{实现类路径}`
**实现方法：** `{实现方法名}`

```java
{实现代码}
```

**运行测试：**
```bash
mvn test -Dtest={测试类}#{测试方法}
```

**实际结果：** ✅ 测试通过

---

### REFACTOR - 重构优化

**重构内容：**
- {重构点 1}
- {重构点 2}

**重构后代码：**
```java
{重构后代码}
```

**运行测试：**
```bash
mvn test -Dtest={测试类}
```

**实际结果：** ✅ 所有测试通过

---

## 下一步
{下一个测试用例或完成}
```

## 示例

### 完整 TDD 循环示例

```markdown
# TDD 实施：缺陷创建功能

## 循环 1：创建缺陷（正常场景）

### RED - 编写测试

**测试类：** `DefectServiceTest.java`
**测试方法：** `testCreateDefect_success`

```java
@Test
public void testCreateDefect_success() {
    // Given
    SysDefect defect = new SysDefect();
    defect.setDefectName("测试缺陷");
    defect.setDefectType("BUG");

    // When
    int result = defectService.insertDefect(defect);

    // Then
    assertEquals(1, result);
    assertNotNull(defect.getDefectId());
}
```

**运行测试：**
```bash
mvn test -Dtest=DefectServiceTest#testCreateDefect_success
```

**预期结果：** ❌ 测试失败（方法未实现）

---

### GREEN - 实现代码

**实现类：** `DefectServiceImpl.java`

```java
@Override
public int insertDefect(SysDefect defect) {
    defect.setCreateTime(DateUtils.getNowDate());
    defect.setCreateBy(SecurityUtils.getUsername());
    return defectMapper.insertDefect(defect);
}
```

**运行测试：**
```bash
mvn test -Dtest=DefectServiceTest#testCreateDefect_success
```

**实际结果：** ✅ 测试通过

---

### REFACTOR - 重构优化

**重构内容：**
- 提取审计字段设置逻辑

**重构后代码：**
```java
@Override
public int insertDefect(SysDefect defect) {
    setAuditFields(defect);
    return defectMapper.insertDefect(defect);
}

private void setAuditFields(SysDefect defect) {
    defect.setCreateTime(DateUtils.getNowDate());
    defect.setCreateBy(SecurityUtils.getUsername());
}
```

**运行测试：**
```bash
mvn test -Dtest=DefectServiceTest
```

**实际结果：** ✅ 所有测试通过

---

## 下一步
继续循环 2：测试缺陷名称为空的场景
```

## 相关命令
- `/plan` - 制定实施计划
- `/build-fix` - 修复构建错误
- `/code-review` - 代码审查
