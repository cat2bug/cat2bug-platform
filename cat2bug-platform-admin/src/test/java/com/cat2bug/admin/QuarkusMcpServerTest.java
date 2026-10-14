package com.cat2bug.admin;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysDefectLog;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.SysUserAccessToken;
import com.cat2bug.common.security.AccessTokens;
import com.cat2bug.framework.security.LoginUserResolver;
import com.cat2bug.framework.security.TokenService;
import com.cat2bug.system.mapper.SysDefectLogMapper;
import com.cat2bug.system.mapper.SysGitRepositoryMapper;
import com.cat2bug.system.mapper.SysUserAccessTokenMapper;
import com.cat2bug.system.service.SysUserAccessTokenService;
import com.cat2bug.system.service.git.GitPathValidator;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class QuarkusMcpServerTest {

    @Inject
    SysDefectLogMapper defectLogMapper;

    @Inject
    SysGitRepositoryMapper repositoryMapper;

    @Inject
    GitPathValidator gitPathValidator;

    @Inject
    SysUserAccessTokenMapper tokenMapper;

    @Inject
    SysUserAccessTokenService tokenService;

    @Inject
    TokenService jwtTokenService;

    @Inject
    LoginUserResolver loginUserResolver;

    private static final String PASSWORD = "Cat2bug@123";

    private String admin;
    private String pat;
    private long projectId;
    private String projectRef;

    @BeforeEach
    void setUp() {
        admin = QuarkusTestAuth.loginAsAdmin();
        String suffix = Long.toString(System.nanoTime(), 36);
        Number teamId = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"teamName\":\"MCP团队" + suffix + "\",\"teamKey\":\"mt" + suffix + "\"}")
                .when().post("/system/team")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.teamId");
        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"teamId\":" + teamId + ",\"projectName\":\"MCP项目" + suffix + "\",\"projectKey\":\"mp" + suffix + "\"}")
                .when().post("/system/project")
                .then().statusCode(200).body("code", equalTo(200));
        projectId = given().header("Authorization", admin)
                .when().get("/system/project/list?teamId=" + teamId + "&pageNum=1&pageSize=10")
                .then().statusCode(200).extract().<Number>path("rows[0].projectId").longValue();
        projectRef = String.valueOf(projectId);
        pat = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"name\":\"MCP 测试 " + suffix + "\",\"expireDays\":1}")
                .when().post("/system/user/profile/tokens")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.token");
    }

    @Test
    void projectSettingsDefaults() {
        given().header("Authorization", admin)
                .when().get("/system/project/" + projectId + "/mcp")
                .then().statusCode(200).body("code", equalTo(200))
                .body("data.enabled", equalTo(true))
                .body("data.modules", hasItem("defect"))
                .body("data.modules", hasItem("code"))
                .body("data.editable", equalTo(true))
                .body("data.mcpUrl", endsWith("/mcp"));

        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"enabled\":true,\"modules\":[\"defect\",\"nope\"]}")
                .when().put("/system/project/" + projectId + "/mcp")
                .then().body("code", not(equalTo(200)));
    }

    @Test
    void unauthenticatedChallengeAndMetadata() {
        given().contentType("application/json").body("{}")
                .when().post("/mcp")
                .then().statusCode(401)
                .header("WWW-Authenticate", containsString("resource_metadata="))
                .header("WWW-Authenticate", containsString("/.well-known/oauth-protected-resource"));
        given().when().get("/.well-known/oauth-protected-resource")
                .then().statusCode(200)
                .body("resource", endsWith("/mcp"))
                .body("bearer_methods_supported", hasItem("header"));
    }

    @Test
    void toolsListedAndDefectFlow() {
        McpTestClient mcp = new McpTestClient(pat).initialize("Cursor-Test");
        List<String> names = new ArrayList<>();
        mcp.listTools().forEach(t -> names.add(t.path("name").asText()));
        assertTrue(names.containsAll(List.of("list_my_projects", "search_defects", "create_defect",
                "repair_defect", "search_code", "call_operation")), names.toString());

        JsonNode projects = McpTestClient.json(mcp.callTool("list_my_projects", Map.of()));
        assertTrue(projects.toString().contains("\"projectId\":" + projectId), projects.toString());

        JsonNode created = mcp.callTool("create_defect", Map.of(
                "project", projectRef, "name", "MCP 新建缺陷", "handle_by", List.of(1),
                "idempotency_key", "mcp-test-" + projectId));
        assertFalse(created.path("isError").asBoolean(), McpTestClient.text(created));
        long defectId = McpTestClient.json(created).path("defectId").asLong();
        assertTrue(defectId > 0, McpTestClient.text(created));

        JsonNode replay = McpTestClient.json(mcp.callTool("create_defect", Map.of(
                "project", projectRef, "name", "MCP 新建缺陷", "idempotency_key", "mcp-test-" + projectId)));
        assertEquals(defectId, replay.path("defectId").asLong());
        assertTrue(replay.path("replayed").asBoolean());

        JsonNode search = McpTestClient.json(mcp.callTool("search_defects",
                Map.of("project", projectRef, "assigned_to_me", true)));
        assertTrue(search.toString().contains("MCP 新建缺陷"), search.toString());

        JsonNode badCommit = mcp.callTool("repair_defect", Map.of("project", projectRef, "defect_id", defectId,
                "remark", "修好了", "commits", List.of(Map.of("sha", "not-a-sha", "url", "https://example.com/c/1"))));
        assertTrue(badCommit.path("isError").asBoolean(), McpTestClient.text(badCommit));

        JsonNode repaired = mcp.callTool("repair_defect", Map.of("project", projectRef, "defect_id", defectId,
                "remark", "修好了", "review_by", 1,
                "commits", List.of(Map.of("sha", "a1b2c3d4e5f6", "url", "https://example.com/commit/a1b2c3d4e5f6",
                        "message", "fix: 外部提交"))));
        assertFalse(repaired.path("isError").asBoolean(), McpTestClient.text(repaired));

        JsonNode wrongState = mcp.callTool("repair_defect", Map.of("project", projectRef, "defect_id", defectId,
                "remark", "再修一次"));
        assertTrue(wrongState.path("isError").asBoolean(), McpTestClient.text(wrongState));

        JsonNode detail = McpTestClient.json(mcp.callTool("get_defect",
                Map.of("project", projectRef, "defect_id", defectId)));
        assertTrue(detail.toString().contains("a1b2c3d4e5f6"), detail.toString());

        List<SysDefectLog> logs = defectLogMapper.selectSysDefectLogList(logQuery(defectId), 0, 50);
        assertTrue(logs.stream().anyMatch(l -> "mcp".equals(l.getOperChannel())
                && "Cursor-Test".equals(l.getClientName())), "日志应记录 MCP 渠道与客户端名");

        given().header("Authorization", admin)
                .when().get("/monitor/operlog/list?pageNum=1&pageSize=20&operChannel=mcp")
                .then().statusCode(200).body("rows.operChannel", hasItem("mcp"));
    }

    @Test
    void ownershipAndModuleChecks() {
        Number foreignDefect = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"defectName\":\"其它项目的缺陷\",\"projectId\":1,\"handleBy\":[1]}")
                .when().post("/system/defect")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.defectId");
        McpTestClient mcp = new McpTestClient(pat).initialize("Claude-Test");

        JsonNode foreign = mcp.callTool("get_defect", Map.of("project", projectRef, "defect_id", foreignDefect));
        assertTrue(foreign.path("isError").asBoolean());
        assertTrue(McpTestClient.text(foreign).contains("不属于该项目"), McpTestClient.text(foreign));

        JsonNode foreignGeneric = mcp.callTool("call_operation", Map.of("project", projectRef,
                "operation", "defect.getInfo", "path_params", Map.of("defectId", foreignDefect)));
        assertTrue(foreignGeneric.path("isError").asBoolean(), McpTestClient.text(foreignGeneric));

        JsonNode ops = McpTestClient.json(mcp.callTool("list_operations", Map.of("project", projectRef, "module", "defect")));
        assertTrue(ops.toString().contains("defect.list"), ops.toString());
        assertFalse(ops.toString().contains("/system/user"), ops.toString());
        assertFalse(ops.toString().contains("/export"), ops.toString());
        assertFalse(ops.toString().contains("importData"), ops.toString());
        JsonNode exportCall = mcp.callTool("call_operation", Map.of("project", projectRef, "operation", "defect.export"));
        assertTrue(exportCall.path("isError").asBoolean(), McpTestClient.text(exportCall));
        JsonNode codeOps = McpTestClient.json(mcp.callTool("list_operations", Map.of("project", projectRef, "module", "code")));
        assertFalse(codeOps.toString().contains("info/refs"), codeOps.toString());
        assertFalse(codeOps.toString().contains("/archive"), codeOps.toString());

        Map<String, Object> generic = new HashMap<>();
        generic.put("project", projectRef);
        generic.put("operation", "defect.list");
        generic.put("query", Map.of("pageNum", 1, "pageSize", 5));
        JsonNode listed = mcp.callTool("call_operation", generic);
        assertFalse(listed.path("isError").asBoolean(), McpTestClient.text(listed));

        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"enabled\":true,\"modules\":[\"case\"]}")
                .when().put("/system/project/" + projectId + "/mcp")
                .then().statusCode(200).body("code", equalTo(200));
        JsonNode blocked = mcp.callTool("search_defects", Map.of("project", projectRef));
        assertTrue(blocked.path("isError").asBoolean(), McpTestClient.text(blocked));

        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"enabled\":false,\"modules\":[\"defect\"]}")
                .when().put("/system/project/" + projectId + "/mcp")
                .then().statusCode(200).body("code", equalTo(200));
        JsonNode disabled = mcp.callTool("search_cases", Map.of("project", projectRef));
        assertTrue(disabled.path("isError").asBoolean(), McpTestClient.text(disabled));
        JsonNode projects = McpTestClient.json(mcp.callTool("list_my_projects", Map.of()));
        assertFalse(projects.toString().contains("\"projectId\":" + projectId), projects.toString());
    }

    @Test
    void codeSearch() throws Exception {
        long repositoryId = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"projectId\":" + projectId + ",\"name\":\"mcp-demo\",\"slug\":\"mcp-demo\"}")
                .when().post("/system/git/repo")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().<Number>path("data.repositoryId").longValue();
        String diskPath = repositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId).getDiskPath();
        String defaultBranch = repositoryMapper.selectSysGitRepositoryByRepositoryId(repositoryId).getDefaultBranch();
        commitFiles(new File(gitPathValidator.resolveStoredDiskPath(diskPath)),
                defaultBranch == null ? "master" : defaultBranch,
                Map.of("src/App.java", "class App {\n  // TODO 修复空指针 NullPointer\n}\n",
                        "README.md", "hello nullpointer\n"));

        given().header("Authorization", admin)
                .when().get("/system/git/repo/" + repositoryId + "/search?q=nullpointer&path=src")
                .then().statusCode(200).body("code", equalTo(200))
                .body("data.matches.path", hasItem("src/App.java"))
                .body("data.matches.path", not(hasItem("README.md")));
        given().header("Authorization", admin)
                .when().get("/system/git/repo/" + repositoryId + "/search?q=")
                .then().body("code", not(equalTo(200)));

        McpTestClient mcp = new McpTestClient(pat).initialize("Cursor-Test");
        JsonNode found = McpTestClient.json(mcp.callTool("search_code", Map.of(
                "project", projectRef, "repository_id", repositoryId, "query", "NullPointer")));
        assertTrue(found.toString().contains("src/App.java") && found.toString().contains("README.md"), found.toString());
        JsonNode file = mcp.callTool("read_code_file", Map.of(
                "project", projectRef, "repository_id", repositoryId, "path", "src/App.java"));
        assertTrue(McpTestClient.text(file).contains("修复空指针"), McpTestClient.text(file));
    }

    @Test
    void tokenBoundToOtherResourceRejected() {
        String plaintext = AccessTokens.generate(AccessTokens.PERSONAL_PREFIX);
        Date now = new Date();
        SysUserAccessToken row = tokenService.newRow(1L, "别的资源", plaintext, SysUserAccessToken.SOURCE_MANUAL,
                new Date(now.getTime() + 3_600_000L), now);
        row.setResource("https://other.example.com/mcp");
        tokenMapper.insert(row);
        given().header("Authorization", "Bearer " + plaintext)
                .when().get("/mcp/__whoami").then().statusCode(401);
    }

    @Test
    void internalSessionNotUsableAsCookieToken() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        LoginUser session = new LoginUser();
        session.setUserId(1L);
        session.setUser(user);
        session.setOperChannel("mcp");
        String token = jwtTokenService.createToken(session);
        try {
            assertNull(loginUserResolver.resolveToken(token));
        } finally {
            jwtTokenService.delLoginUser(session.getToken());
        }
    }

    private static void commitFiles(File gitDir, String branch, Map<String, String> files) throws Exception {
        try (Repository repo = new FileRepositoryBuilder().setGitDir(gitDir).build();
             ObjectInserter inserter = repo.newObjectInserter()) {
            Map<String, Map<String, ObjectId>> dirs = new java.util.TreeMap<>();
            for (Map.Entry<String, String> e : files.entrySet()) {
                int slash = e.getKey().lastIndexOf('/');
                String dir = slash < 0 ? "" : e.getKey().substring(0, slash);
                ObjectId blob = inserter.insert(Constants.OBJ_BLOB, e.getValue().getBytes(StandardCharsets.UTF_8));
                dirs.computeIfAbsent(dir, k -> new java.util.TreeMap<>()).put(e.getKey().substring(slash + 1), blob);
            }
            TreeFormatter root = new TreeFormatter();
            Map<String, ObjectId> rootEntries = new java.util.TreeMap<>(dirs.getOrDefault("", Map.of()));
            Map<String, ObjectId> subTrees = new java.util.TreeMap<>();
            for (Map.Entry<String, Map<String, ObjectId>> d : dirs.entrySet()) {
                if (d.getKey().isEmpty()) {
                    continue;
                }
                TreeFormatter sub = new TreeFormatter();
                d.getValue().forEach((name, id) -> sub.append(name, FileMode.REGULAR_FILE, id));
                subTrees.put(d.getKey(), inserter.insert(sub));
            }
            java.util.TreeMap<String, Object[]> all = new java.util.TreeMap<>();
            rootEntries.forEach((n, id) -> all.put(n, new Object[]{FileMode.REGULAR_FILE, id}));
            subTrees.forEach((n, id) -> all.put(n, new Object[]{FileMode.TREE, id}));
            all.forEach((n, v) -> root.append(n, (FileMode) v[0], (ObjectId) v[1]));
            ObjectId treeId = inserter.insert(root);
            CommitBuilder commit = new CommitBuilder();
            commit.setTreeId(treeId);
            PersonIdent ident = new PersonIdent("tester", "tester@example.com");
            commit.setAuthor(ident);
            commit.setCommitter(ident);
            commit.setMessage("init");
            ObjectId commitId = inserter.insert(commit);
            inserter.flush();
            RefUpdate update = repo.updateRef(Constants.R_HEADS + branch);
            update.setNewObjectId(commitId);
            update.forceUpdate();
            repo.updateRef(Constants.HEAD).link(Constants.R_HEADS + branch);
        }
    }

    @Test
    void roleIntersectionAndWebProjectUntouched() {
        long webProject = 1L;
        QuarkusTestAuth.ensureCurrentProject(admin, webProject);
        String suffix = Long.toString(System.nanoTime(), 36);
        String userName = "mcpo_" + suffix;
        long userId = createUser(userName);
        try {
            QuarkusTestAuth.ensureProjectMember(admin, projectId, userId);
            given().header("Authorization", admin).contentType(ContentType.JSON)
                    .body("[9]")
                    .when().put("/system/project/" + projectId + "/member/" + userId + "/role")
                    .then().statusCode(200).body("code", equalTo(200));
            String userJwt = login(userName);
            QuarkusTestAuth.ensureCurrentProject(userJwt, projectId);
            userJwt = login(userName);
            String userPat = createPat(userJwt, "外部成员令牌");

            given().header("Authorization", userJwt)
                    .when().get("/system/project/" + projectId + "/mcp")
                    .then().statusCode(200).body("code", equalTo(200))
                    .body("data.editable", equalTo(false));
            given().header("Authorization", userJwt).contentType(ContentType.JSON)
                    .body("{\"enabled\":false,\"modules\":[]}")
                    .when().put("/system/project/" + projectId + "/mcp")
                    .then().body("code", not(equalTo(200)));

            McpTestClient mcp = new McpTestClient(userPat).initialize("OpenCode-Test");
            JsonNode created = mcp.callTool("create_defect", Map.of("project", projectRef, "name", "外部成员提的缺陷"));
            assertFalse(created.path("isError").asBoolean(), McpTestClient.text(created));
            JsonNode caseDenied = mcp.callTool("create_case", Map.of("project", projectRef, "name", "无权限用例", "module_id", 1));
            assertTrue(caseDenied.path("isError").asBoolean(), McpTestClient.text(caseDenied));
            assertTrue(McpTestClient.text(caseDenied).contains("无权限"), McpTestClient.text(caseDenied));

            JsonNode ops = McpTestClient.json(mcp.callTool("list_operations", Map.of("project", projectRef)));
            assertTrue(ops.toString().contains("defect.list"), ops.toString());
            assertFalse(ops.toString().contains("\"case.add\""), ops.toString());
            assertFalse(ops.toString().contains("project.edit"), ops.toString());

            McpTestClient adminMcp = new McpTestClient(pat).initialize("Cursor-Test");
            adminMcp.callTool("search_defects", Map.of("project", projectRef));
            given().header("Authorization", admin)
                    .when().get("/getInfo")
                    .then().statusCode(200)
                    .body("config.currentProjectId", equalTo((int) webProject));
        } finally {
            given().header("Authorization", admin).when().delete("/system/user/" + userId);
        }
    }

    @Test
    void writeToolsSucceed() {
        McpTestClient mcp = new McpTestClient(pat).initialize("Cursor-Test");
        long defectId = McpTestClient.json(ok(mcp.callTool("create_defect", Map.of(
                "project", projectRef, "name", "写工具缺陷", "handle_by", List.of(1))))).path("defectId").asLong();
        Map<String, Object> target = Map.of("project", projectRef, "defect_id", defectId);

        ok(mcp.callTool("update_defect", with(target, "name", "写工具缺陷-改")));
        ok(mcp.callTool("comment_defect", with(target, "content", "MCP 评论")));
        ok(mcp.callTool("assign_defect", with(with(target, "handle_by", List.of(1)), "remark", "转给自己")));
        ok(mcp.callTool("repair_defect", with(with(target, "review_by", 1), "remark", "已修复")));
        ok(mcp.callTool("reject_defect", with(target, "remark", "没修好")));
        ok(mcp.callTool("repair_defect", with(with(target, "review_by", 1), "remark", "再修")));
        ok(mcp.callTool("pass_defect", with(target, "remark", "通过")));
        ok(mcp.callTool("open_defect", with(with(target, "handle_by", List.of(1)), "remark", "复现了")));
        ok(mcp.callTool("close_defect", with(target, "remark", "关闭")));
        JsonNode detail = McpTestClient.json(mcp.callTool("get_defect", target));
        assertTrue(detail.toString().contains("写工具缺陷-改"), detail.toString());

        Number moduleId = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"projectId\":" + projectId + ",\"modulePid\":0,\"moduleName\":\"MCP模块\"}")
                .when().post("/system/module")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data[0].moduleId");
        long caseId = McpTestClient.json(ok(mcp.callTool("create_case", Map.of("project", projectRef,
                "name", "MCP 用例", "module_id", moduleId,
                "steps", List.of(Map.of("describe", "打开页面", "expect", "页面正常")))))).findValue("caseId").asLong();
        ok(mcp.callTool("update_case", Map.of("project", projectRef, "case_id", caseId, "name", "MCP 用例-改")));

        String planId = given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"planName\":\"MCP计划\",\"planVersion\":\"1.0\",\"projectId\":" + projectId
                        + ",\"sysPlanItemList\":[{\"caseId\":" + caseId + "}]}")
                .when().post("/system/plan")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.planId");
        JsonNode items = McpTestClient.json(ok(mcp.callTool("list_plan_items",
                Map.of("project", projectRef, "plan_id", planId))));
        String planItemId = items.findValue("planItemId").asText();
        ok(mcp.callTool("update_plan_item", Map.of("project", projectRef, "plan_item_id", planItemId,
                "state", "not_pass", "defect_id", defectId)));
    }

    @Test
    void writeToolsDenied() {
        McpTestClient adminMcp = new McpTestClient(pat).initialize("Cursor-Test");
        long defectId = McpTestClient.json(ok(adminMcp.callTool("create_defect", Map.of(
                "project", projectRef, "name", "拒绝用缺陷", "handle_by", List.of(1))))).path("defectId").asLong();
        Map<String, Object> target = Map.of("project", projectRef, "defect_id", defectId);

        String suffix = Long.toString(System.nanoTime(), 36);
        String userName = "mcpd_" + suffix;
        long userId = createUser(userName);
        try {
            QuarkusTestAuth.ensureProjectMember(admin, projectId, userId);
            given().header("Authorization", admin).contentType(ContentType.JSON).body("[9]")
                    .when().put("/system/project/" + projectId + "/member/" + userId + "/role")
                    .then().statusCode(200).body("code", equalTo(200));
            McpTestClient outsider = new McpTestClient(createPat(login(userName), "拒绝")).initialize("Deny-Test");
            Map<String, Map<String, Object>> denied = new java.util.LinkedHashMap<>();
            denied.put("assign_defect", with(with(target, "handle_by", List.of(1)), "remark", "x"));
            denied.put("repair_defect", with(target, "remark", "x"));
            denied.put("reject_defect", with(target, "remark", "x"));
            denied.put("pass_defect", with(target, "remark", "x"));
            denied.put("close_defect", with(target, "remark", "x"));
            denied.put("open_defect", with(with(target, "handle_by", List.of(1)), "remark", "x"));
            denied.put("create_case", Map.of("project", projectRef, "name", "x", "module_id", 1));
            denied.put("update_case", Map.of("project", projectRef, "case_id", 1, "name", "x"));
            denied.put("update_plan_item", Map.of("project", projectRef, "plan_item_id", "x", "state", "pass"));
            denied.forEach((tool, args) -> assertTrue(outsider.callTool(tool, args).path("isError").asBoolean(),
                    tool + " 应因角色权限不足被拒绝"));
        } finally {
            given().header("Authorization", admin).when().delete("/system/user/" + userId);
        }

        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"enabled\":true,\"modules\":[\"case\"]}")
                .when().put("/system/project/" + projectId + "/mcp")
                .then().statusCode(200).body("code", equalTo(200));
        JsonNode create = adminMcp.callTool("create_defect", Map.of("project", projectRef, "name", "x"));
        assertTrue(create.path("isError").asBoolean(), McpTestClient.text(create));
        JsonNode update = adminMcp.callTool("update_defect", with(target, "name", "x"));
        assertTrue(update.path("isError").asBoolean(), McpTestClient.text(update));
        JsonNode comment = adminMcp.callTool("comment_defect", with(target, "content", "x"));
        assertTrue(comment.path("isError").asBoolean(), McpTestClient.text(comment));
    }

    private static JsonNode ok(JsonNode result) {
        assertFalse(result.path("isError").asBoolean(), McpTestClient.text(result));
        return result;
    }

    private static Map<String, Object> with(Map<String, Object> base, String key, Object value) {
        Map<String, Object> m = new HashMap<>(base);
        m.put(key, value);
        return m;
    }

    @Test
    void rateLimitedPerUser() {
        String suffix = Long.toString(System.nanoTime(), 36);
        String userName = "mcpr_" + suffix;
        long userId = createUser(userName);
        try {
            McpTestClient mcp = new McpTestClient(createPat(login(userName), "限流")).initialize("Limit-Test");
            JsonNode last = null;
            for (int i = 0; i < 121; i++) {
                last = mcp.callTool("list_my_projects", Map.of());
            }
            assertTrue(last.path("isError").asBoolean(), McpTestClient.text(last));
            assertTrue(McpTestClient.text(last).contains("过于频繁"), McpTestClient.text(last));
            JsonNode other = new McpTestClient(pat).initialize("Cursor-Test").callTool("list_my_projects", Map.of());
            assertFalse(other.path("isError").asBoolean(), "限流按用户计数，不影响其他用户");
        } finally {
            given().header("Authorization", admin).when().delete("/system/user/" + userId);
        }
    }

    private long createUser(String userName) {
        given().header("Authorization", admin).contentType(ContentType.JSON)
                .body("{\"userName\":\"" + userName + "\",\"nickName\":\"" + userName + "\",\"password\":\""
                        + PASSWORD + "\",\"status\":\"0\"}")
                .when().post("/system/user")
                .then().statusCode(200).body("code", equalTo(200));
        return given().header("Authorization", admin)
                .queryParam("userName", userName)
                .when().get("/system/user/list?pageNum=1&pageSize=10")
                .then().statusCode(200)
                .extract().jsonPath().getLong("rows.find { it.userName == '" + userName + "' }.userId");
    }

    private static String login(String userName) {
        return given().contentType(ContentType.JSON)
                .body("{\"username\":\"" + userName + "\",\"password\":\"" + PASSWORD + "\"}")
                .when().post("/login")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("token");
    }

    private static String createPat(String jwt, String name) {
        return given().header("Authorization", jwt).contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"expireDays\":1}")
                .when().post("/system/user/profile/tokens")
                .then().statusCode(200).body("code", equalTo(200))
                .extract().path("data.token");
    }

    private static SysDefectLog logQuery(long defectId) {
        SysDefectLog q = new SysDefectLog();
        q.setDefectId(defectId);
        return q;
    }
}
