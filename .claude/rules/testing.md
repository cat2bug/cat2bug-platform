# 测试规则

## 测试底线

### 必须测试的场景
- ✅ 新增 API 接口必须有单元测试
- ✅ 核心业务逻辑（Service 层）必须有测试
- ✅ 修改现有逻辑时，必须确保原有测试通过
- ✅ Bug 修复必须补充回归测试

### 测试覆盖率
- 目标：Service 层覆盖率 > 70%
- 关键路径（登录、权限、支付等）覆盖率 > 90%

## TDD 流程

使用 `/tdd` 命令时，严格遵循：

### RED - 先写失败的测试
```java
@Test
public void testCreateDefect() {
    // Given
    SysDefect defect = new SysDefect();
    defect.setDefectName("测试缺陷");

    // When
    int result = defectService.insertDefect(defect);

    // Then
    assertEquals(1, result);
    assertNotNull(defect.getDefectId());
}
```

### GREEN - 实现最小可用代码
- 只写让测试通过的代码
- 不过度设计

### REFACTOR - 重构优化
- 消除重复代码
- 提取公共方法
- 优化命名

## 测试组织

### 后端测试
```
src/test/java/
└── com/cat2bug/
    ├── system/
    │   ├── service/
    │   │   └── DefectServiceTest.java
    │   └── mapper/
    │       └── DefectMapperTest.java
    └── web/
        └── controller/
            └── DefectControllerTest.java
```

### 测试命名
- 测试类：`{ClassName}Test`
- 测试方法：`test{MethodName}_{Scenario}` 或 `should{ExpectedBehavior}_when{Condition}`

### 测试数据
- ✅ 使用 `@Transactional` 自动回滚
- ✅ 使用 H2 内存数据库进行测试
- ❌ 避免依赖外部服务（使用 Mock）

## 运行测试

### 本地开发
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=DefectServiceTest

# 运行特定测试方法
mvn test -Dtest=DefectServiceTest#testCreateDefect

# 跳过测试（仅构建时，不推荐）
mvn clean package -DskipTests
```

### 持续集成
- 每次提交前必须运行测试
- 测试失败禁止合并

## Mock 使用

### 何时使用 Mock
- 外部 API 调用
- 数据库操作（Controller 测试时）
- 复杂依赖

### 示例
```java
@MockBean
private SysDefectMapper defectMapper;

@Test
public void testGetDefect() {
    // Given
    SysDefect mockDefect = new SysDefect();
    mockDefect.setDefectId(1L);
    when(defectMapper.selectDefectById(1L)).thenReturn(mockDefect);

    // When
    SysDefect result = defectService.selectDefectById(1L);

    // Then
    assertNotNull(result);
    assertEquals(1L, result.getDefectId());
}
```

## 前端测试

目前项目未配置前端自动化测试，手动测试时：
- ✅ 测试主要业务流程
- ✅ 测试边界情况（空值、超长输入等）
- ✅ 测试不同浏览器兼容性

## 测试审查清单

- [ ] 新增功能有测试
- [ ] 测试覆盖正常和异常场景
- [ ] 测试可独立运行（不依赖顺序）
- [ ] 测试数据会自动清理
- [ ] 所有测试通过
