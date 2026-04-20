# API Key Capability Switches Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-API-key `apiEnabled` and `mcpEnabled` switches so `/api/**` and `/mcp` can be allowed or blocked independently with `403` responses.

**Architecture:** Extend `sys_project_api` as the single source of truth for API-key capabilities, then carry those flags through the API-key authentication path into centralized request filtering. Keep CRUD behavior unchanged except for exposing the new fields in backend and Vue API Key management screens, and default both flags to enabled for backward compatibility.

**Tech Stack:** Spring Boot 2.5, Spring Security, MyBatis XML mappers, Java 8, Vue 2 + Element UI, Flyway SQL migrations, JUnit4 + Mockito, Maven.

---

## File Structure

### Existing files to modify

- `cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/SysProjectApi.java` — add `apiEnabled` / `mcpEnabled` fields to the admin-side entity.
- `cat2bug-platform-api/src/main/java/com/cat2bug/api/domain/ApiProjectApi.java` — add the same fields to the API-side entity used by API services.
- `cat2bug-platform-system/src/main/java/com/cat2bug/system/mapper/SysProjectApiMapper.java` — mapper contract stays aligned with the new fields.
- `cat2bug-platform-system/src/main/resources/mapper/system/SysProjectApiMapper.xml` — map, insert, and update the two new columns.
- `cat2bug-platform-system/src/main/java/com/cat2bug/system/service/impl/SysProjectApiServiceImpl.java` — set default values for new keys before insert.
- `cat2bug-platform-api/src/main/resources/mapper/ApiProjectApiMapper.xml` — fetch project capability flags with the project lookup if needed by API-side services.
- `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/web/service/ApiUserDetailsServiceImpl.java` — attach capability flags to the authenticated API-key principal.
- `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/ApiSecurityConfig.java` — register any new API capability filter in the `/api/**` chain.
- `cat2bug-platform-common/src/main/java/com/cat2bug/common/core/domain/model/LoginUser.java` — hold API-key capability flags on the authenticated principal.
- `cat2bug-platform-ui/src/views/system/api/index.vue` — add list columns and form switches for API/MCP enablement.
- `cat2bug-platform-ui/src/api/system/api.js` — no schema logic, but validate that requests carry the new booleans unchanged.
- `cat2bug-platform-ui/src/utils/i18n/i18n-zh-CN.json` — add switch and status labels.
- `cat2bug-platform-ui/src/utils/i18n/i18n-en-US.json` — add English switch and status labels.
- `cat2bug-platform-admin/src/main/resources/h2-schema.sql` — update local schema snapshot.
- `cat2bug-platform-admin/src/main/resources/db/migration/mysql/V0_5_1__init.sql` — update MySQL bootstrap schema.
- `cat2bug-platform-admin/src/main/resources/db/migration/h2/V0_5_1__init.sql` — update H2 bootstrap schema.
- `sql/cat2bug_platform.sql` — update repository SQL snapshot.
- `cat2bug-platform-framework/src/test/java/com/cat2bug/framework/web/service/ApiUserDetailsServiceTest.java` — verify API-key principal flags.
- `readme/MCP.md` — document `mcpEnabled` behavior and 403 rule.
- `readme/API.md` — document `apiEnabled` behavior and 403 rule.

### New files to create

- `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter/ApiCapabilityFilter.java` — centralized `/api/**` capability gate returning `403` when API access is disabled.
- `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter/McpCapabilityFilter.java` — centralized `/mcp` capability gate returning `403` when MCP access is disabled.
- `cat2bug-platform-framework/src/test/java/com/cat2bug/framework/security/filter/ApiCapabilityFilterTest.java` — unit tests for `/api/**` gate behavior.
- `cat2bug-platform-framework/src/test/java/com/cat2bug/framework/security/filter/McpCapabilityFilterTest.java` — unit tests for `/mcp` gate behavior.

---

### Task 1: Extend API key data model and persistence

**Files:**
- Modify: `cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/SysProjectApi.java`
- Modify: `cat2bug-platform-api/src/main/java/com/cat2bug/api/domain/ApiProjectApi.java`
- Modify: `cat2bug-platform-system/src/main/resources/mapper/system/SysProjectApiMapper.xml`
- Modify: `cat2bug-platform-system/src/main/java/com/cat2bug/system/service/impl/SysProjectApiServiceImpl.java`
- Modify: `cat2bug-platform-admin/src/main/resources/h2-schema.sql`
- Modify: `cat2bug-platform-admin/src/main/resources/db/migration/mysql/V0_5_1__init.sql`
- Modify: `cat2bug-platform-admin/src/main/resources/db/migration/h2/V0_5_1__init.sql`
- Modify: `sql/cat2bug_platform.sql`

- [ ] **Step 1: Add failing persistence assertions to the existing API-key service tests or create focused mapper/service coverage**

```java
@Test
public void testInsertSysProjectApiDefaultsCapabilityFlagsToTrue() {
    SysProjectApi api = new SysProjectApi();
    api.setApiName("demo key");
    api.setProjectId(1L);

    sysProjectApiService.insertSysProjectApi(api);

    ArgumentCaptor<SysProjectApi> captor = ArgumentCaptor.forClass(SysProjectApi.class);
    verify(sysProjectApiMapper).insertSysProjectApi(captor.capture());
    assertEquals(Boolean.TRUE, captor.getValue().getApiEnabled());
    assertEquals(Boolean.TRUE, captor.getValue().getMcpEnabled());
}
```

- [ ] **Step 2: Run the targeted test to confirm it fails before implementation**

Run:

```bash
mvn -B -pl cat2bug-platform-system -am test -Dtest=SysProjectApiServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because `SysProjectApi` does not yet expose `apiEnabled` / `mcpEnabled` and the service does not populate them.

- [ ] **Step 3: Add the two boolean fields to both entities**

```java
/** 是否开启 API */
@Excel(name = "是否开启API")
private Boolean apiEnabled;

/** 是否开启 MCP */
@Excel(name = "是否开启MCP")
private Boolean mcpEnabled;
```

Apply the same field names in:
- `com.cat2bug.system.domain.SysProjectApi`
- `com.cat2bug.api.domain.ApiProjectApi`

- [ ] **Step 4: Update MyBatis result mapping and SQL write paths**

```xml
<result property="apiEnabled" column="api_enabled" />
<result property="mcpEnabled" column="mcp_enabled" />
```

Add both columns to:
- `selectSysProjectApiVo`
- `insertSysProjectApi`
- `updateSysProjectApi`

Use the exact XML shape:

```xml
<if test="apiEnabled != null">api_enabled,</if>
<if test="mcpEnabled != null">mcp_enabled,</if>
```

and

```xml
<if test="apiEnabled != null">#{apiEnabled},</if>
<if test="mcpEnabled != null">#{mcpEnabled},</if>
```

- [ ] **Step 5: Default both flags to enabled in the service layer for new keys**

```java
if (apiProjectApi.getApiEnabled() == null) {
    apiProjectApi.setApiEnabled(Boolean.TRUE);
}
if (apiProjectApi.getMcpEnabled() == null) {
    apiProjectApi.setMcpEnabled(Boolean.TRUE);
}
```

Place this in `SysProjectApiServiceImpl.insertSysProjectApi(...)` before `insertSysProjectApi(...)` is called.

- [ ] **Step 6: Update schema bootstrap files so fresh environments include the new columns**

Use these exact column definitions in all four SQL files:

```sql
`api_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否开启API',
`mcp_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否开启MCP',
```

For existing `CREATE TABLE sys_project_api` statements, insert the columns after `remark` or immediately before the primary key and preserve the file’s quoting/style.

- [ ] **Step 7: Add a forward migration for existing databases if this repo already uses incremental migrations beyond init-only bootstrap**

Create one migration per supported DB only if the project’s Flyway setup expects additive migrations instead of re-running init scripts. Use:

```sql
ALTER TABLE sys_project_api ADD COLUMN api_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否开启API';
ALTER TABLE sys_project_api ADD COLUMN mcp_enabled tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否开启MCP';
UPDATE sys_project_api SET api_enabled = 1 WHERE api_enabled IS NULL;
UPDATE sys_project_api SET mcp_enabled = 1 WHERE mcp_enabled IS NULL;
```

If no incremental migration convention exists here, skip creating a new file and keep the change in the bootstrap SQL files only.

- [ ] **Step 8: Re-run the targeted persistence test**

Run:

```bash
mvn -B -pl cat2bug-platform-system -am test -Dtest=SysProjectApiServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with assertions proving new keys default to enabled.

- [ ] **Step 9: Commit the model/persistence slice**

```bash
git add cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/SysProjectApi.java cat2bug-platform-api/src/main/java/com/cat2bug/api/domain/ApiProjectApi.java cat2bug-platform-system/src/main/resources/mapper/system/SysProjectApiMapper.xml cat2bug-platform-system/src/main/java/com/cat2bug/system/service/impl/SysProjectApiServiceImpl.java cat2bug-platform-admin/src/main/resources/h2-schema.sql cat2bug-platform-admin/src/main/resources/db/migration/mysql/V0_5_1__init.sql cat2bug-platform-admin/src/main/resources/db/migration/h2/V0_5_1__init.sql sql/cat2bug_platform.sql

git commit -m "feat: add API key capability columns"
```

### Task 2: Carry capability flags through API-key authentication

**Files:**
- Modify: `cat2bug-platform-common/src/main/java/com/cat2bug/common/core/domain/model/LoginUser.java`
- Modify: `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/web/service/ApiUserDetailsServiceImpl.java`
- Modify: `cat2bug-platform-framework/src/test/java/com/cat2bug/framework/web/service/ApiUserDetailsServiceTest.java`

- [ ] **Step 1: Add failing authentication tests for the new flags**

Extend `ApiUserDetailsServiceTest` with:

```java
@Test
public void testCreateLoginUser_ProjectApi_CopiesCapabilityFlags() {
    projectApi.setApiEnabled(Boolean.FALSE);
    projectApi.setMcpEnabled(Boolean.TRUE);
    when(sysProjectApiMapper.selectSysProjectApiByApiId("project_api_key_123")).thenReturn(projectApi);

    LoginUser loginUser = apiUserDetailsService.createLoginUser("project_api_key_123");

    assertNotNull(loginUser);
    assertEquals(Boolean.FALSE, loginUser.getApiEnabled());
    assertEquals(Boolean.TRUE, loginUser.getMcpEnabled());
}

@Test
public void testCreateLoginUser_ProjectApi_DefaultsMissingFlagsToTrue() {
    projectApi.setApiEnabled(null);
    projectApi.setMcpEnabled(null);
    when(sysProjectApiMapper.selectSysProjectApiByApiId("project_api_key_123")).thenReturn(projectApi);

    LoginUser loginUser = apiUserDetailsService.createLoginUser("project_api_key_123");

    assertEquals(Boolean.TRUE, loginUser.getApiEnabled());
    assertEquals(Boolean.TRUE, loginUser.getMcpEnabled());
}
```

- [ ] **Step 2: Run the authentication test class first**

Run:

```bash
mvn -B -pl cat2bug-platform-framework -am test -Dtest=ApiUserDetailsServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because `LoginUser` has no capability fields yet.

- [ ] **Step 3: Add capability fields to `LoginUser`**

```java
private Boolean apiEnabled;
private Boolean mcpEnabled;

public Boolean getApiEnabled() {
    return apiEnabled;
}

public void setApiEnabled(Boolean apiEnabled) {
    this.apiEnabled = apiEnabled;
}

public Boolean getMcpEnabled() {
    return mcpEnabled;
}

public void setMcpEnabled(Boolean mcpEnabled) {
    this.mcpEnabled = mcpEnabled;
}
```

- [ ] **Step 4: Populate those fields during API-key authentication**

In `ApiUserDetailsServiceImpl.createLoginUser(...)`, after loading `SysProjectApi`:

```java
user.setApiEnabled(sysProjectApi.getApiEnabled() == null ? Boolean.TRUE : sysProjectApi.getApiEnabled());
user.setMcpEnabled(sysProjectApi.getMcpEnabled() == null ? Boolean.TRUE : sysProjectApi.getMcpEnabled());
```

Also set robot keys to both `Boolean.TRUE` so robot tokens keep current behavior:

```java
user.setApiEnabled(Boolean.TRUE);
user.setMcpEnabled(Boolean.TRUE);
```

- [ ] **Step 5: Re-run the authentication tests**

Run:

```bash
mvn -B -pl cat2bug-platform-framework -am test -Dtest=ApiUserDetailsServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with flag values copied onto the authenticated principal.

- [ ] **Step 6: Commit the authentication propagation slice**

```bash
git add cat2bug-platform-common/src/main/java/com/cat2bug/common/core/domain/model/LoginUser.java cat2bug-platform-framework/src/main/java/com/cat2bug/framework/web/service/ApiUserDetailsServiceImpl.java cat2bug-platform-framework/src/test/java/com/cat2bug/framework/web/service/ApiUserDetailsServiceTest.java

git commit -m "feat: propagate API key capability flags"
```

### Task 3: Add centralized `/api/**` capability enforcement

**Files:**
- Create: `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter/ApiCapabilityFilter.java`
- Create: `cat2bug-platform-framework/src/test/java/com/cat2bug/framework/security/filter/ApiCapabilityFilterTest.java`
- Modify: `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/ApiSecurityConfig.java`

- [ ] **Step 1: Write the failing `/api/**` capability filter tests**

```java
@RunWith(MockitoJUnitRunner.class)
public class ApiCapabilityFilterTest {
    @InjectMocks
    private ApiCapabilityFilter filter;
    @Mock
    private FilterChain filterChain;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    public void testAllowsRequestWhenApiEnabled() throws Exception {
        LoginUser loginUser = new LoginUser();
        loginUser.setApiEnabled(Boolean.TRUE);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
        when(request.getRequestURI()).thenReturn("/api/report/push");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testReturns403WhenApiDisabled() throws Exception {
        LoginUser loginUser = new LoginUser();
        loginUser.setApiEnabled(Boolean.FALSE);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
        when(request.getRequestURI()).thenReturn("/api/report/push");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(403);
        verify(filterChain, never()).doFilter(request, response);
    }
}
```

- [ ] **Step 2: Run the new filter test**

Run:

```bash
mvn -B -pl cat2bug-platform-framework -am test -Dtest=ApiCapabilityFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because `ApiCapabilityFilter` does not exist.

- [ ] **Step 3: Implement the `/api/**` capability filter**

```java
@Component
@ConditionalOnProperty(prefix = "cat2bug.api", name = "enabled", havingValue = "true")
public class ApiCapabilityFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) principal;
            if (Boolean.FALSE.equals(loginUser.getApiEnabled())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(AjaxResult.error(403, "API capability is disabled for this API key")));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: Register the filter after authentication in the API security chain**

In `ApiSecurityConfig.configure(HttpSecurity httpSecurity)` add:

```java
httpSecurity.addFilterAfter(apiCapabilityFilter, ApiAuthenticationTokenFilter.class);
```

and inject it:

```java
@Autowired
private ApiCapabilityFilter apiCapabilityFilter;
```

- [ ] **Step 5: Re-run the targeted filter test**

Run:

```bash
mvn -B -pl cat2bug-platform-framework -am test -Dtest=ApiCapabilityFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with one allow case and one 403 case.

- [ ] **Step 6: Commit the `/api/**` enforcement slice**

```bash
git add cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter/ApiCapabilityFilter.java cat2bug-platform-framework/src/test/java/com/cat2bug/framework/security/filter/ApiCapabilityFilterTest.java cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/ApiSecurityConfig.java

git commit -m "feat: block disabled API keys on api routes"
```

### Task 4: Add centralized `/mcp` capability enforcement

**Files:**
- Create: `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter/McpCapabilityFilter.java`
- Create: `cat2bug-platform-framework/src/test/java/com/cat2bug/framework/security/filter/McpCapabilityFilterTest.java`
- `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/ApiSecurityConfig.java`

- [ ] **Step 1: Locate the actual `/mcp` HTTP entrypoint before coding**

Run:

```bash
rg -n '"/mcp"|McpProtocolService|jsonrpc|tools/call|tools/list|initialize' cat2bug-platform-* --glob '*.java' --glob '*.xml' --glob '*.yml' --glob '*.properties'
```

Expected: one controller, servlet, handler, or registration class that owns `/mcp` request handling.

If this command returns no matches in the current worktree, stop here and first sync the missing MCP server source from the branch/commit where `/mcp` was already implemented. Do not invent a new transport layer for this task; continue only after the existing MCP entrypoint file is present and identified.

- [ ] **Step 2: Write the failing `/mcp` capability filter tests**

```java
@RunWith(MockitoJUnitRunner.class)
public class McpCapabilityFilterTest {
    @InjectMocks
    private McpCapabilityFilter filter;
    @Mock
    private FilterChain filterChain;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    public void testAllowsRequestWhenMcpEnabled() throws Exception {
        LoginUser loginUser = new LoginUser();
        loginUser.setMcpEnabled(Boolean.TRUE);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
        when(request.getRequestURI()).thenReturn("/mcp");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testReturns403WhenMcpDisabled() throws Exception {
        LoginUser loginUser = new LoginUser();
        loginUser.setMcpEnabled(Boolean.FALSE);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
        when(request.getRequestURI()).thenReturn("/mcp");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(403);
        verify(filterChain, never()).doFilter(request, response);
    }
}
```

- [ ] **Step 3: Run the new filter test first**

Run:

```bash
mvn -B -pl cat2bug-platform-framework -am test -Dtest=McpCapabilityFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because `McpCapabilityFilter` does not exist yet.

- [ ] **Step 4: Implement the MCP capability filter**

Use the same pattern as the API filter, but read `loginUser.getMcpEnabled()` and return:

```java
AjaxResult.error(403, "MCP capability is disabled for this API key")
```

- [ ] **Step 5: Register the MCP filter at the real `/mcp` entrypoint discovered in Step 1**

If Step 1 identifies that `/mcp` is already protected by the same Spring Security API-key chain as `/api/**`, register the filter in `ApiSecurityConfig` with:

```java
httpSecurity.addFilterAfter(mcpCapabilityFilter, ApiCapabilityFilter.class);
```

If Step 1 instead identifies a servlet/controller/interceptor registration outside `ApiSecurityConfig`, register `McpCapabilityFilter` there using the framework style already present in that file. Do not invent a new transport or duplicate authentication logic.

- [ ] **Step 6: Re-run the MCP filter test**

Run:

```bash
mvn -B -pl cat2bug-platform-framework -am test -Dtest=McpCapabilityFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with explicit allow and 403 cases.

- [ ] **Step 7: Commit the `/mcp` enforcement slice**

```bash
git add cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/filter/McpCapabilityFilter.java cat2bug-platform-framework/src/test/java/com/cat2bug/framework/security/filter/McpCapabilityFilterTest.java cat2bug-platform-framework/src/main/java/com/cat2bug/framework/config/ApiSecurityConfig.java

git commit -m "feat: block disabled API keys on mcp route"
```

### Task 5: Expose the switches in API key management UI

**Files:**
- Modify: `cat2bug-platform-ui/src/views/system/api/index.vue`
- Modify: `cat2bug-platform-ui/src/utils/i18n/i18n-zh-CN.json`
- Modify: `cat2bug-platform-ui/src/utils/i18n/i18n-en-US.json`

- [ ] **Step 1: Add a failing frontend test or at minimum define the expected form/list state before editing**

Use this expected Vue form shape as the contract:

```js
this.form = {
  apiId: null,
  projectId: this.projectId,
  userId: null,
  whiteList: null,
  expireTime: null,
  remark: null,
  apiName: null,
  apiEnabled: true,
  mcpEnabled: true
}
```

If this repo has no frontend unit-test harness for this page, record the contract in code first and verify manually in later steps.

- [ ] **Step 2: Add two table columns showing capability state**

Insert these columns into `index.vue`:

```vue
<el-table-column :label="$t('project.api.api-status')" align="center" prop="apiEnabled">
  <template slot-scope="scope">
    <el-tag :type="scope.row.apiEnabled ? 'success' : 'danger'">
      {{ scope.row.apiEnabled ? $t('project.api.enabled') : $t('project.api.disabled') }}
    </el-tag>
  </template>
</el-table-column>
<el-table-column :label="$t('project.api.mcp-status')" align="center" prop="mcpEnabled">
  <template slot-scope="scope">
    <el-tag :type="scope.row.mcpEnabled ? 'success' : 'danger'">
      {{ scope.row.mcpEnabled ? $t('project.api.enabled') : $t('project.api.disabled') }}
    </el-tag>
  </template>
</el-table-column>
```

- [ ] **Step 3: Add two switch controls to the create/edit dialog**

```vue
<el-form-item :label="$t('project.api.api-enabled')" prop="apiEnabled">
  <el-switch v-model="form.apiEnabled"></el-switch>
</el-form-item>
<el-form-item :label="$t('project.api.mcp-enabled')" prop="mcpEnabled">
  <el-switch v-model="form.mcpEnabled"></el-switch>
</el-form-item>
```

- [ ] **Step 4: Make the form reset path default both switches to enabled**

```js
reset() {
  this.form = {
    apiId: null,
    projectId: this.projectId,
    userId: null,
    whiteList: null,
    expireTime: null,
    remark: null,
    apiName: null,
    apiEnabled: true,
    mcpEnabled: true
  }
  this.resetForm('form')
}
```

- [ ] **Step 5: Add i18n labels for the new controls and status tags**

Add these keys to both Chinese and English files:

```json
"project.api.api-enabled": "是否开启 API",
"project.api.mcp-enabled": "是否开启 MCP",
"project.api.api-status": "API 状态",
"project.api.mcp-status": "MCP 状态",
"project.api.enabled": "开启",
"project.api.disabled": "关闭"
```

English values:

```json
"project.api.api-enabled": "Enable API",
"project.api.mcp-enabled": "Enable MCP",
"project.api.api-status": "API Status",
"project.api.mcp-status": "MCP Status",
"project.api.enabled": "Enabled",
"project.api.disabled": "Disabled"
```

- [ ] **Step 6: Verify CRUD payloads manually in the browser or devtools**

Manual check list:
- create dialog opens with both switches on
- edit dialog shows saved values
- submit payload includes `apiEnabled` and `mcpEnabled`
- list view renders correct status tags after save

- [ ] **Step 7: Commit the UI slice**

```bash
git add cat2bug-platform-ui/src/views/system/api/index.vue cat2bug-platform-ui/src/utils/i18n/i18n-zh-CN.json cat2bug-platform-ui/src/utils/i18n/i18n-en-US.json

git commit -m "feat: manage API key capability switches"
```

### Task 6: Verify end-to-end behavior and update docs

**Files:**
- Modify: `readme/MCP.md`
- Modify: `readme/API.md`

- [ ] **Step 1: Add documentation notes for the two switches**

Add the exact behavior summary:

```md
- API Key 现在支持两个能力开关：`apiEnabled`、`mcpEnabled`
- 当 `apiEnabled = false` 时，请求 `/api/**` 返回 `403`
- 当 `mcpEnabled = false` 时，请求 `/mcp` 返回 `403`
- 新建 API Key 默认两个开关都为开启
```

Also update any request examples or setup notes that describe API Key behavior as all-or-nothing.

- [ ] **Step 2: Run focused backend tests for the changed areas**

Run:

```bash
mvn -B -pl cat2bug-platform-framework -am test -Dtest=ApiUserDetailsServiceTest,ApiCapabilityFilterTest,McpCapabilityFilterTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS with all capability propagation and filter tests green.

- [ ] **Step 3: Run a compile pass across affected modules**

Run:

```bash
mvn -B -pl cat2bug-platform-admin,cat2bug-platform-framework,cat2bug-platform-system,cat2bug-platform-api -am compile -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Manually verify HTTP behavior with one enabled key and one disabled key**

Use `curl` with real keys created from the UI.

API allow case:

```bash
curl --location 'http://127.0.0.1:2020/api/project/version' \
-H 'CAT2BUG-API-KEY: <api-enabled-key>'
```

Expected: non-403 response.

API deny case:

```bash
curl --location 'http://127.0.0.1:2020/api/project/version' \
-H 'CAT2BUG-API-KEY: <api-disabled-key>'
```

Expected: HTTP 403 with JSON error body.

MCP allow case:

```bash
curl --location 'http://127.0.0.1:2020/mcp' \
-H 'Content-Type: application/json' \
-H 'CAT2BUG-API-KEY: <mcp-enabled-key>' \
-d '{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {}
}'
```

Expected: JSON-RPC success response.

MCP deny case:

```bash
curl --location 'http://127.0.0.1:2020/mcp' \
-H 'Content-Type: application/json' \
-H 'CAT2BUG-API-KEY: <mcp-disabled-key>' \
-d '{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {}
}'
```

Expected: HTTP 403.

- [ ] **Step 5: Check that historical rows remain enabled after migration**

Run against the configured MySQL database:

```bash
/opt/homebrew/opt/mysql-client/bin/mysql -h 127.0.0.1 -P 3306 -u root -pcat2bug_password -D cat2bug_platform -e "SELECT api_id, api_enabled, mcp_enabled FROM sys_project_api LIMIT 10;"
```

Expected: existing rows show `1, 1` unless a user explicitly changed them.

- [ ] **Step 6: Commit docs and verification artifacts**

```bash
git add readme/MCP.md readme/API.md

git commit -m "docs: describe API key capability switches"
```

---

## Final Verification Checklist

- [ ] `SysProjectApi` and `ApiProjectApi` both expose `apiEnabled` / `mcpEnabled`
- [ ] `sys_project_api` schema includes `api_enabled` / `mcp_enabled` with default `1`
- [ ] new API keys default both flags to enabled
- [ ] existing API-key CRUD APIs return and accept both flags
- [ ] `LoginUser` carries both flags after API-key authentication
- [ ] `/api/**` returns `403` when `apiEnabled = false`
- [ ] `/mcp` returns `403` when `mcpEnabled = false`
- [ ] UI create/edit form supports both switches and defaults them to enabled
- [ ] UI list view shows both statuses clearly
- [ ] `readme/MCP.md` and `readme/API.md` describe the new behavior
