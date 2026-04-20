# Defect MCP Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add six MCP defect action tools (`defect.assign`, `defect.reject`, `defect.repair`, `defect.pass`, `defect.close`, `defect.open`) that reuse existing defect service flows and return the unified object envelope.

**Architecture:** Keep MCP as a thin adapter layer in `cat2bug-platform-admin`: resolve `defectNum` in the API-key-bound project, resolve handler accounts to project member IDs when required, build `SysDefectLog`, then delegate to `ISysDefectService`. Share common parsing and lookup logic in a focused abstract base handler so each concrete tool class only defines schema and the service method it calls.

**Tech Stack:** Spring Boot 2.5, Java 8, existing MCP protocol layer, MyBatis mappers (`SysUserMapper`, `SysUserProjectMapper`), JUnit4, Mockito, Maven.

---

## File Structure

### New files

- `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/AbstractDefectActionMcpToolHandler.java` — shared lookup/parsing helper for defect actions
- `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectAssignMcpToolHandler.java` — `defect.assign`
- `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectRejectMcpToolHandler.java` — `defect.reject`
- `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectRepairMcpToolHandler.java` — `defect.repair`
- `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectPassMcpToolHandler.java` — `defect.pass`
- `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectCloseMcpToolHandler.java` — `defect.close`
- `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectOpenMcpToolHandler.java` — `defect.open`
- `cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/AbstractDefectActionMcpToolHandlerTest.java` — shared lookup/validation tests
- `cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/DefectActionMcpToolHandlersTest.java` — concrete handler schema and service delegation tests

### Existing files to modify

- `cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/McpProtocolServiceTest.java` — add authorization and structured result coverage for a new defect action tool
- `readme/MCP.md` — document the six new tools and their parameters
- `readme/API.md` — mirror the new action tool list and request examples

---

### Task 1: Build shared defect action lookup helper

**Files:**
- Create: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/AbstractDefectActionMcpToolHandler.java`
- Create: `cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/AbstractDefectActionMcpToolHandlerTest.java`
- Read for reference: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectGetMcpToolHandler.java`
- Read for reference: `cat2bug-platform-system/src/main/java/com/cat2bug/system/mapper/SysUserMapper.java`
- Read for reference: `cat2bug-platform-system/src/main/java/com/cat2bug/system/mapper/SysUserProjectMapper.java`
- Read for reference: `cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/SysDefectLog.java`

- [ ] **Step 1: Write the failing shared helper tests**

```java
package com.cat2bug.web.mcp.tool;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.web.mcp.McpExecutionContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractDefectActionMcpToolHandlerTest {
    @Mock
    private IApiDefectService apiDefectService;
    @Mock
    private SysUserMapper sysUserMapper;

    private TestHandler handler;
    private McpExecutionContext context;

    @Before
    public void setUp() {
        handler = new TestHandler();
        ReflectionTestUtils.setField(handler, "apiDefectService", apiDefectService);
        ReflectionTestUtils.setField(handler, "sysUserMapper", sysUserMapper);
        context = new McpExecutionContext(null, 1L, Collections.singleton("api:defect:edit"));
    }

    @Test
    public void testBuildLogResolvesDefectAndHandlers() {
        ApiDefect defect = new ApiDefect();
        defect.setDefectId(100L);
        SysUser demo = new SysUser();
        demo.setUserId(7L);
        demo.setUserName("demo");
        when(apiDefectService.selectSysDefectByDefectNumber(3L)).thenReturn(defect);
        when(sysUserMapper.selectUserByUserName(eq(null), eq(1L), eq("demo"))).thenReturn(demo);

        SysDefectLog log = handler.build(context, 3L, "note", Arrays.asList("demo"), true);

        assertEquals(Long.valueOf(100L), log.getDefectId());
        assertEquals("note", log.getDefectLogDescribe());
        assertEquals(Collections.singletonList(7L), log.getReceiveBy());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildLogRejectsMissingHandlerListWhenRequired() {
        ApiDefect defect = new ApiDefect();
        defect.setDefectId(100L);
        when(apiDefectService.selectSysDefectByDefectNumber(3L)).thenReturn(defect);

        handler.build(context, 3L, null, Collections.<String>emptyList(), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildLogRejectsUnknownHandlerAccount() {
        ApiDefect defect = new ApiDefect();
        defect.setDefectId(100L);
        when(apiDefectService.selectSysDefectByDefectNumber(3L)).thenReturn(defect);
        when(sysUserMapper.selectUserByUserName(eq(null), eq(1L), eq("ghost"))).thenReturn(null);

        handler.build(context, 3L, null, Collections.singletonList("ghost"), true);
    }

    private static class TestHandler extends AbstractDefectActionMcpToolHandler {
        SysDefectLog build(McpExecutionContext context, Long defectNum, String describe, java.util.List<String> handlerAccountList, boolean requireHandlers) {
            return buildDefectLog(context, defectNum, describe, handlerAccountList, requireHandlers);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=AbstractDefectActionMcpToolHandlerTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL with compilation errors because `AbstractDefectActionMcpToolHandler` does not exist.

- [ ] **Step 3: Write the minimal shared helper implementation**

```java
package com.cat2bug.web.mcp.tool;

import com.cat2bug.api.domain.ApiDefect;
import com.cat2bug.api.service.IApiDefectService;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.web.mcp.McpExecutionContext;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractDefectActionMcpToolHandler {
    @Resource
    protected IApiDefectService apiDefectService;
    @Resource
    protected SysUserMapper sysUserMapper;

    protected SysDefectLog buildDefectLog(McpExecutionContext context, Long defectNum, String defectLogDescribe,
                                          List<String> handlerAccountList, boolean requireHandlers) {
        if (defectNum == null) {
            throw new IllegalArgumentException("defectNum is required");
        }
        ApiDefect defect = this.apiDefectService.selectSysDefectByDefectNumber(defectNum);
        if (defect == null || defect.getDefectId() == null) {
            throw new IllegalArgumentException("defect not found");
        }
        List<Long> receiverIds = this.resolveHandlerIds(context, handlerAccountList, requireHandlers);
        SysDefectLog log = new SysDefectLog();
        log.setDefectId(defect.getDefectId());
        log.setDefectLogDescribe(defectLogDescribe);
        log.setReceiveBy(receiverIds);
        return log;
    }

    protected Long asRequiredLong(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    protected String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    protected List<String> asRequiredStringList(Object value, String message) {
        if (!(value instanceof List) || ((List<?>) value).isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return ((List<?>) value).stream().map(String::valueOf).collect(Collectors.toList());
    }

    protected List<String> asOptionalStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("handlerAccountList must be an array");
        }
        return ((List<?>) value).stream().map(String::valueOf).collect(Collectors.toList());
    }

    private List<Long> resolveHandlerIds(McpExecutionContext context, List<String> handlerAccountList, boolean requireHandlers) {
        if (requireHandlers) {
            handlerAccountList = this.asRequiredHandlerAccounts(handlerAccountList);
        }
        if (handlerAccountList == null || handlerAccountList.isEmpty()) {
            return null;
        }
        return handlerAccountList.stream().map(account -> {
            SysUser user = this.sysUserMapper.selectUserByUserName(null, context.getProjectId(), account);
            if (user == null || user.getUserId() == null) {
                throw new IllegalArgumentException("handler account not found in project: " + account);
            }
            return user.getUserId();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<String> asRequiredHandlerAccounts(List<String> handlerAccountList) {
        if (handlerAccountList == null || handlerAccountList.isEmpty()) {
            throw new IllegalArgumentException("handlerAccountList is required");
        }
        return handlerAccountList;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=AbstractDefectActionMcpToolHandlerTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with `Tests run: 3, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/AbstractDefectActionMcpToolHandler.java cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/AbstractDefectActionMcpToolHandlerTest.java
git commit -m "feat: add shared MCP defect action helper"
```

### Task 2: Add three handler-required defect action tools

**Files:**
- Create: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectAssignMcpToolHandler.java`
- Create: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectRepairMcpToolHandler.java`
- Create: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectOpenMcpToolHandler.java`
- Create: `cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/DefectActionMcpToolHandlersTest.java`

- [ ] **Step 1: Write the failing handler tests**

```java
package com.cat2bug.web.mcp.tool;

import com.cat2bug.system.domain.SysDefectLog;
import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.web.mcp.McpExecutionContext;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefectActionMcpToolHandlersTest {
    @Mock
    private ISysDefectService sysDefectService;

    private McpExecutionContext context;

    @Before
    public void setUp() {
        context = new McpExecutionContext(null, 1L, Collections.singleton("api:defect:edit"));
    }

    @Test
    public void testAssignDefinitionRequiresHandlers() {
        DefectAssignMcpToolHandler handler = new DefectAssignMcpToolHandler();
        McpSchema.Tool tool = handler.definition();
        assertEquals("defect.assign", tool.name());
        assertTrue(tool.inputSchema().required().contains("handlerAccountList"));
    }

    @Test
    public void testRepairDelegatesToService() {
        DefectRepairMcpToolHandler handler = new DefectRepairMcpToolHandler();
        ReflectionTestUtils.setField(handler, "sysDefectService", sysDefectService);
        ReflectionTestUtils.setField(handler, "apiDefectService", new StubApiDefectService(200L));
        ReflectionTestUtils.setField(handler, "sysUserMapper", new StubSysUserMapper(8L, "demo"));
        SysDefectLog returned = new SysDefectLog();
        returned.setDefectId(200L);
        when(sysDefectService.repair(any(SysDefectLog.class))).thenReturn(returned);
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("defectNum", 9);
        arguments.put("handlerAccountList", Arrays.asList("demo"));

        Object result = handler.handle(context, arguments);

        assertTrue(result instanceof SysDefectLog);
        assertEquals(Long.valueOf(200L), ((SysDefectLog) result).getDefectId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=DefectActionMcpToolHandlersTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because the new handler classes do not exist.

- [ ] **Step 3: Implement assign/repair/open handlers**

```java
package com.cat2bug.web.mcp.tool;

import com.cat2bug.system.service.ISysDefectService;
import com.cat2bug.web.mcp.McpExecutionContext;
import com.cat2bug.web.mcp.McpToolHandler;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefectAssignMcpToolHandler extends AbstractDefectActionMcpToolHandler implements McpToolHandler {
    @Resource
    private ISysDefectService sysDefectService;

    @Override
    public Object handle(McpExecutionContext context, Map<String, Object> arguments) {
        return this.sysDefectService.assign(buildDefectLog(
                context,
                asRequiredLong(arguments.get("defectNum"), "defectNum is required"),
                asString(arguments.get("defectLogDescribe")),
                asRequiredStringList(arguments.get("handlerAccountList"), "handlerAccountList is required"),
                true
        ));
    }

    @Override
    public McpSchema.Tool definition() {
        return McpSchema.Tool.builder()
                .name("defect.assign")
                .description("指派当前项目缺陷")
                .inputSchema(schema(true))
                .meta(meta())
                .build();
    }

    private McpSchema.JsonSchema schema(boolean requireHandlers) {
        Map<String, Object> properties = baseProperties(requireHandlers);
        return new McpSchema.JsonSchema("object", properties,
                Arrays.asList("defectNum", "handlerAccountList"), Boolean.FALSE, null, null);
    }

    private Map<String, Object> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("permission", "api:defect:edit");
        return meta;
    }
}
```

`DefectRepairMcpToolHandler` and `DefectOpenMcpToolHandler` follow the same structure, changing only the tool name, description, and delegated service call (`repair`, `open`). Add the shared `baseProperties(boolean requireHandlers)` helper to `AbstractDefectActionMcpToolHandler`:

```java
protected Map<String, Object> baseProperties(boolean requireHandlers) {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("defectNum", property("integer", "缺陷编号"));
    properties.put("defectLogDescribe", property("string", "操作说明"));
    if (requireHandlers) {
        Map<String, Object> handler = property("array", "处理人账号列表");
        handler.put("items", Collections.singletonMap("type", "string"));
        properties.put("handlerAccountList", handler);
    }
    return properties;
}

protected Map<String, Object> property(String type, String description) {
    Map<String, Object> property = new LinkedHashMap<>();
    property.put("type", type);
    property.put("description", description);
    return property;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=DefectActionMcpToolHandlersTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with `Tests run: 2, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/AbstractDefectActionMcpToolHandler.java cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectAssignMcpToolHandler.java cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectRepairMcpToolHandler.java cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectOpenMcpToolHandler.java cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/DefectActionMcpToolHandlersTest.java
git commit -m "feat: add MCP defect handlers with assignee resolution"
```

### Task 3: Add reject/pass/close action tools

**Files:**
- Create: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectRejectMcpToolHandler.java`
- Create: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectPassMcpToolHandler.java`
- Create: `cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectCloseMcpToolHandler.java`
- Modify: `cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/DefectActionMcpToolHandlersTest.java`

- [ ] **Step 1: Extend the handler tests with no-handler actions**

```java
@Test
public void testRejectDefinitionDoesNotRequireHandlers() {
    DefectRejectMcpToolHandler handler = new DefectRejectMcpToolHandler();
    McpSchema.Tool tool = handler.definition();
    assertEquals("defect.reject", tool.name());
    assertTrue(tool.inputSchema().required().contains("defectNum"));
    assertTrue(!tool.inputSchema().required().contains("handlerAccountList"));
}

@Test
public void testCloseDelegatesToServiceWithoutHandlers() {
    DefectCloseMcpToolHandler handler = new DefectCloseMcpToolHandler();
    ReflectionTestUtils.setField(handler, "sysDefectService", sysDefectService);
    ReflectionTestUtils.setField(handler, "apiDefectService", new StubApiDefectService(200L));
    ReflectionTestUtils.setField(handler, "sysUserMapper", new StubSysUserMapper(8L, "demo"));
    SysDefectLog returned = new SysDefectLog();
    returned.setDefectId(200L);
    when(sysDefectService.close(any(SysDefectLog.class))).thenReturn(returned);
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("defectNum", 9);
    arguments.put("defectLogDescribe", "close it");

    Object result = handler.handle(context, arguments);

    assertTrue(result instanceof SysDefectLog);
    assertEquals(Long.valueOf(200L), ((SysDefectLog) result).getDefectId());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=DefectActionMcpToolHandlersTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because the reject/pass/close handlers do not exist.

- [ ] **Step 3: Implement reject/pass/close handlers**

```java
@Component
public class DefectRejectMcpToolHandler extends AbstractDefectActionMcpToolHandler implements McpToolHandler {
    @Resource
    private ISysDefectService sysDefectService;

    @Override
    public Object handle(McpExecutionContext context, Map<String, Object> arguments) {
        return this.sysDefectService.reject(buildDefectLog(
                context,
                asRequiredLong(arguments.get("defectNum"), "defectNum is required"),
                asString(arguments.get("defectLogDescribe")),
                Collections.<String>emptyList(),
                false
        ));
    }

    @Override
    public McpSchema.Tool definition() {
        return McpSchema.Tool.builder()
                .name("defect.reject")
                .description("驳回当前项目缺陷")
                .inputSchema(new McpSchema.JsonSchema("object", baseProperties(false),
                        Collections.singletonList("defectNum"), Boolean.FALSE, null, null))
                .meta(meta())
                .build();
    }

    private Map<String, Object> meta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("permission", "api:defect:edit");
        return meta;
    }
}
```

`DefectPassMcpToolHandler` and `DefectCloseMcpToolHandler` follow the same pattern, changing only the tool name, description, and service method (`pass`, `close`).

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=DefectActionMcpToolHandlersTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with `Tests run: 4, Failures: 0, Errors: 0`.

- [ ] **Step 5: Commit**

```bash
git add cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectRejectMcpToolHandler.java cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectPassMcpToolHandler.java cat2bug-platform-admin/src/main/java/com/cat2bug/web/mcp/tool/DefectCloseMcpToolHandler.java cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/tool/DefectActionMcpToolHandlersTest.java
git commit -m "feat: add MCP defect state transition tools"
```

### Task 4: Extend protocol coverage for new tools

**Files:**
- Modify: `cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/McpProtocolServiceTest.java`

- [ ] **Step 1: Write the failing protocol tests for a new action tool**

```java
@Test
public void testToolsListIncludesDefectAssignTool() {
    McpExecutionContext editContext = new McpExecutionContext(new LoginUser(), 1L, Collections.singleton("api:defect:edit"));
    when(contextResolver.resolve()).thenReturn(editContext);
    when(toolRegistry.listAuthorizedTools(editContext)).thenReturn(Arrays.asList(
            McpSchema.Tool.builder().name("defect.assign").description("assign").inputSchema(new McpSchema.JsonSchema("object", Collections.emptyMap(), Collections.emptyList(), Boolean.FALSE, null, null)).meta(Collections.singletonMap("permission", "api:defect:edit")).build()
    ));
    McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", "tools/list", 6, Collections.emptyMap());

    McpSchema.JSONRPCResponse response = protocolService.handle(request);

    McpSchema.ListToolsResult result = (McpSchema.ListToolsResult) response.result();
    assertEquals("defect.assign", result.tools().get(0).name());
}

@Test
public void testToolsCallReturnsObjectEnvelopeForDefectAssign() {
    McpExecutionContext editContext = new McpExecutionContext(new LoginUser(), 1L, Collections.singleton("api:defect:edit"));
    when(contextResolver.resolve()).thenReturn(editContext);
    McpToolRegistration registration = new McpToolRegistration(
            "defect.assign",
            "api:defect:edit",
            McpSchema.Tool.builder().name("defect.assign").description("assign").inputSchema(new McpSchema.JsonSchema("object", Collections.emptyMap(), Collections.emptyList(), Boolean.FALSE, null, null)).meta(Collections.singletonMap("permission", "api:defect:edit")).build(),
            toolHandler
    );
    when(toolRegistry.get("defect.assign")).thenReturn(registration);
    when(toolHandler.handle(org.mockito.ArgumentMatchers.eq(editContext), anyMap())).thenReturn(Collections.singletonMap("defectLogType", "ASSIGN"));
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("name", "defect.assign");
    params.put("arguments", Collections.singletonMap("defectNum", 1));
    McpSchema.JSONRPCRequest request = new McpSchema.JSONRPCRequest("2.0", "tools/call", 7, params);

    McpSchema.JSONRPCResponse response = protocolService.handle(request);

    McpSchema.CallToolResult result = (McpSchema.CallToolResult) response.result();
    Map<?, ?> structuredContent = (Map<?, ?>) result.structuredContent();
    assertEquals("object", structuredContent.get("type"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=McpProtocolServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because `McpProtocolServiceTest` still uses a context limited to `api:defect:list`.

- [ ] **Step 3: Update protocol test contexts and assertions**

```java
@Before
public void setUp() {
    protocolService = new McpProtocolService();
    ReflectionTestUtils.setField(protocolService, "contextResolver", contextResolver);
    ReflectionTestUtils.setField(protocolService, "toolRegistry", toolRegistry);
    ReflectionTestUtils.setField(protocolService, "resultAdapter", new McpResultAdapter());
    context = new McpExecutionContext(new LoginUser(), 1L, Collections.singleton("api:defect:list"));
}
```

Keep the existing list context for list-specific tests, but create a local `editContext` inside the new action tests for `api:defect:edit`. Do not broaden the shared context and accidentally weaken the unauthorized-tool assertion.

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=McpProtocolServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with all protocol tests green.

- [ ] **Step 5: Commit**

```bash
git add cat2bug-platform-admin/src/test/java/com/cat2bug/web/mcp/McpProtocolServiceTest.java
git commit -m "test: cover MCP defect action protocol flow"
```

### Task 5: Document and manually verify the six new tools

**Files:**
- Modify: `readme/MCP.md`
- Modify: `readme/API.md`

- [ ] **Step 1: Add the six new tools to both docs with exact request examples**

```md
- `defect.assign`
- `defect.reject`
- `defect.repair`
- `defect.pass`
- `defect.close`
- `defect.open`
```

Add one full example in `readme/MCP.md` for `defect.assign`:

```bash
curl --location 'http://127.0.0.1:2020/mcp' \
-H 'Content-Type: application/json' \
-H 'CAT2BUG-API-KEY: 你的项目API_KEY' \
-d '{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "tools/call",
  "params": {
    "name": "defect.assign",
    "arguments": {
      "defectNum": 1,
      "handlerAccountList": ["demo"],
      "defectLogDescribe": "重新指派给 demo"
    }
  }
}'
```

Add one full example in `readme/API.md` for `defect.pass`:

```bash
curl --location 'http://127.0.0.1:2020/mcp' \
-H 'Content-Type: application/json' \
-H 'CAT2BUG-API-KEY: 你的项目API_KEY' \
-d '{
  "jsonrpc": "2.0",
  "id": 11,
  "method": "tools/call",
  "params": {
    "name": "defect.pass",
    "arguments": {
      "defectNum": 1,
      "defectLogDescribe": "验证通过"
    }
  }
}'
```

- [ ] **Step 2: Run the focused unit tests and compile before manual verification**

Run:

```bash
mvn -B -pl cat2bug-platform-admin -am test -Dtest=AbstractDefectActionMcpToolHandlerTest,DefectActionMcpToolHandlersTest,McpProtocolServiceTest -Dsurefire.failIfNoSpecifiedTests=false
mvn -B -pl cat2bug-platform-admin -am compile -DskipTests
```

Expected:
- tests PASS
- compile PASS

- [ ] **Step 3: Start or restart the admin service**

Run one of:

```bash
mvn -B -f cat2bug-platform-admin/pom.xml spring-boot:run
```

or, if already packaged and using the jar flow:

```bash
java -jar cat2bug-platform-admin/target/cat2bug-admin.jar
```

Expected: service listens on `http://127.0.0.1:2020` with no startup errors.

- [ ] **Step 4: Verify all six MCP action tools manually**

Run:

```bash
curl -s -X POST "http://127.0.0.1:2020/mcp" -H "Content-Type: application/json" -H "CAT2BUG-API-KEY: 你的项目API_KEY" --data '{"jsonrpc":"2.0","id":901,"method":"tools/call","params":{"name":"defect.assign","arguments":{"defectNum":1,"handlerAccountList":["demo"],"defectLogDescribe":"重新指派给 demo"}}}'
curl -s -X POST "http://127.0.0.1:2020/mcp" -H "Content-Type: application/json" -H "CAT2BUG-API-KEY: 你的项目API_KEY" --data '{"jsonrpc":"2.0","id":902,"method":"tools/call","params":{"name":"defect.repair","arguments":{"defectNum":1,"handlerAccountList":["demo"],"defectLogDescribe":"已修复，请验证"}}}'
curl -s -X POST "http://127.0.0.1:2020/mcp" -H "Content-Type: application/json" -H "CAT2BUG-API-KEY: 你的项目API_KEY" --data '{"jsonrpc":"2.0","id":903,"method":"tools/call","params":{"name":"defect.pass","arguments":{"defectNum":1,"defectLogDescribe":"验证通过"}}}'
curl -s -X POST "http://127.0.0.1:2020/mcp" -H "Content-Type: application/json" -H "CAT2BUG-API-KEY: 你的项目API_KEY" --data '{"jsonrpc":"2.0","id":904,"method":"tools/call","params":{"name":"defect.reject","arguments":{"defectNum":1,"defectLogDescribe":"回归失败"}}}'
curl -s -X POST "http://127.0.0.1:2020/mcp" -H "Content-Type: application/json" -H "CAT2BUG-API-KEY: 你的项目API_KEY" --data '{"jsonrpc":"2.0","id":905,"method":"tools/call","params":{"name":"defect.close","arguments":{"defectNum":1,"defectLogDescribe":"无须继续处理"}}}'
curl -s -X POST "http://127.0.0.1:2020/mcp" -H "Content-Type: application/json" -H "CAT2BUG-API-KEY: 你的项目API_KEY" --data '{"jsonrpc":"2.0","id":906,"method":"tools/call","params":{"name":"defect.open","arguments":{"defectNum":1,"handlerAccountList":["demo"],"defectLogDescribe":"重新开启并指派"}}}'
```

Expected for each response:
- `result.error = false`
- `result.isError = false`
- `result.structuredContent.type = "object"`
- `result.structuredContent.item.defectLogType` matches the action (`ASSIGN`, `REPAIR`, `PASS`, `REJECTED`, `CLOSED`, `OPEN`)

- [ ] **Step 5: Commit**

```bash
git add readme/MCP.md readme/API.md
git commit -m "docs: add MCP defect action usage examples"
```

## Self-Review

- Spec coverage checked: every required tool, shared lookup, handler-account rule, permission reuse, object envelope, tests, and docs has a task.
- Placeholder scan checked: no `TODO`, `TBD`, or “similar to task N” instructions are left as placeholders.
- Type consistency checked: the plan consistently uses `defectNum`, `handlerAccountList`, `defectLogDescribe`, `SysDefectLog`, and `api:defect:edit` across all tasks.
