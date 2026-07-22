package com.cat2bug.system.mapper;

import com.cat2bug.system.domain.SysGitRepository;
import com.cat2bug.system.git.browse.domain.GitProjectOverview;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link SysGitRepositoryMapper#selectProjectOverviewCounts} /
 * {@link SysGitRepositoryMapper#selectRepositoriesForOverviewScan} 的真实 SQL 验证
 * （OpenSpec {@code git-repo-project-overview} 任务 1.4）：不启 Spring，秒级跑真实 H2 SQL，
 * 覆盖「多用户关注合计」「open PR 过滤」「已停用项目下的仓库不计入可见集」等场景。
 */
public class SysGitRepositoryMapperOverviewTest {

    private static final long VISIBLE_PROJECT_ID = 100L;
    private static final long DISABLED_PROJECT_ID = 200L;
    private static final long EMPTY_PROJECT_ID = 300L;

    private Connection connection;
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:git_repo_overview_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        connection = dataSource.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE sys_project (project_id BIGINT PRIMARY KEY, project_state INT)");
            stmt.execute("CREATE TABLE sys_git_repository ("
                    + "repository_id BIGINT PRIMARY KEY, project_id BIGINT, name VARCHAR(100), slug VARCHAR(100),"
                    + "disk_path VARCHAR(500), default_branch VARCHAR(100), visibility VARCHAR(20),"
                    + "create_by VARCHAR(64), create_time TIMESTAMP, update_by VARCHAR(64), update_time TIMESTAMP,"
                    + "remark VARCHAR(500))");
            stmt.execute("CREATE TABLE sys_git_pull_request ("
                    + "pull_request_id BIGINT PRIMARY KEY, repository_id BIGINT, state VARCHAR(16))");
            stmt.execute("CREATE TABLE sys_user_git_repository ("
                    + "user_git_repository_id BIGINT PRIMARY KEY, user_id BIGINT, repository_id BIGINT, collect INT)");

            // 项目：100=运行中（可见），200=已停用（project_state=0，不可见），300=运行中但无仓
            stmt.execute("INSERT INTO sys_project VALUES (" + VISIBLE_PROJECT_ID + ", 1)");
            stmt.execute("INSERT INTO sys_project VALUES (" + DISABLED_PROJECT_ID + ", 0)");
            stmt.execute("INSERT INTO sys_project VALUES (" + EMPTY_PROJECT_ID + ", 1)");

            // 可见项目下 2 仓；已停用项目下 1 仓（不应计入任何统计）
            stmt.execute("INSERT INTO sys_git_repository (repository_id, project_id, name, slug, disk_path, default_branch) "
                    + "VALUES (1, " + VISIBLE_PROJECT_ID + ", 'repo-a', 'repo-a', '/data/repo-a.git', 'main')");
            stmt.execute("INSERT INTO sys_git_repository (repository_id, project_id, name, slug, disk_path, default_branch) "
                    + "VALUES (2, " + VISIBLE_PROJECT_ID + ", 'repo-b', 'repo-b', '/data/repo-b.git', 'main')");
            stmt.execute("INSERT INTO sys_git_repository (repository_id, project_id, name, slug, disk_path, default_branch) "
                    + "VALUES (3, " + DISABLED_PROJECT_ID + ", 'repo-c', 'repo-c', '/data/repo-c.git', 'main')");

            // repo-a：2 open + 1 merged；repo-b：1 open；repo-c（不可见）：1 open，不应计入
            stmt.execute("INSERT INTO sys_git_pull_request VALUES (1, 1, 'open')");
            stmt.execute("INSERT INTO sys_git_pull_request VALUES (2, 1, 'open')");
            stmt.execute("INSERT INTO sys_git_pull_request VALUES (3, 1, 'merged')");
            stmt.execute("INSERT INTO sys_git_pull_request VALUES (4, 2, 'open')");
            stmt.execute("INSERT INTO sys_git_pull_request VALUES (5, 3, 'open')");

            // repo-a 被 user1、user2 关注（collect=1，跨用户各计一行）；repo-b 被 user3 关注、user4 取消关注（collect=0，不计）
            stmt.execute("INSERT INTO sys_user_git_repository VALUES (1, 10, 1, 1)");
            stmt.execute("INSERT INTO sys_user_git_repository VALUES (2, 20, 1, 1)");
            stmt.execute("INSERT INTO sys_user_git_repository VALUES (3, 30, 2, 1)");
            stmt.execute("INSERT INTO sys_user_git_repository VALUES (4, 40, 2, 0)");
            // repo-c（不可见）关注也不应计入
            stmt.execute("INSERT INTO sys_user_git_repository VALUES (5, 50, 3, 1)");
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("h2-git-repo-overview", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultScriptingLanguage(XMLLanguageDriver.class);
        configuration.getTypeAliasRegistry().registerAliases("com.cat2bug.system.domain");
        configuration.getTypeAliasRegistry().registerAliases("com.cat2bug.system.git.browse.domain");
        configuration.addMapper(SysGitRepositoryMapper.class);
        configuration.setDatabaseId("h2");

        loadMapper(configuration, "mapper/system/SysGitRepositoryMapper.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private static void loadMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(resource + " not on classpath", inputStream);
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void selectProjectOverviewCounts_aggregatesRepoOpenPrAndMultiUserCollectSum() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysGitRepositoryMapper mapper = session.getMapper(SysGitRepositoryMapper.class);
            GitProjectOverview counts = mapper.selectProjectOverviewCounts(VISIBLE_PROJECT_ID);

            assertNotNull(counts);
            assertEquals(2L, counts.getRepoCount());
            // repo-a 2 open + repo-b 1 open = 3；repo-a 的 1 条 merged 与 repo-c 的 open 均不计入
            assertEquals(3L, counts.getOpenPrCount());
            // repo-a 两个用户 collect=1（各计一行）+ repo-b 一个用户 collect=1；repo-b 的 collect=0 不计；repo-c 不可见不计
            assertEquals(3L, counts.getCollectCount());
        }
    }

    @Test
    public void selectProjectOverviewCounts_emptyProject_returnsZerosNotNull() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysGitRepositoryMapper mapper = session.getMapper(SysGitRepositoryMapper.class);
            GitProjectOverview counts = mapper.selectProjectOverviewCounts(EMPTY_PROJECT_ID);

            assertNotNull(counts);
            assertEquals(0L, counts.getRepoCount());
            assertEquals(0L, counts.getOpenPrCount());
            assertEquals(0L, counts.getCollectCount());
        }
    }

    @Test
    public void selectProjectOverviewCounts_disabledProjectRepos_excludedFromCounts() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysGitRepositoryMapper mapper = session.getMapper(SysGitRepositoryMapper.class);
            GitProjectOverview counts = mapper.selectProjectOverviewCounts(DISABLED_PROJECT_ID);

            // sys_project.project_state = 0：与仓库列表同一可见集判定，一律视为不可见
            assertNotNull(counts);
            assertEquals(0L, counts.getRepoCount());
            assertEquals(0L, counts.getOpenPrCount());
            assertEquals(0L, counts.getCollectCount());
        }
    }

    @Test
    public void selectRepositoriesForOverviewScan_returnsOnlyVisibleRepositories() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysGitRepositoryMapper mapper = session.getMapper(SysGitRepositoryMapper.class);
            List<SysGitRepository> repos = mapper.selectRepositoriesForOverviewScan(VISIBLE_PROJECT_ID);

            List<Long> ids = repos.stream().map(SysGitRepository::getRepositoryId).collect(Collectors.toList());
            assertEquals(2, repos.size());
            assertTrue(ids.contains(1L));
            assertTrue(ids.contains(2L));
            assertFalse(ids.contains(3L));
            // 语言扫描所需字段随行返回
            SysGitRepository repoA = repos.stream().filter(r -> r.getRepositoryId() == 1L).findFirst().orElse(null);
            assertNotNull(repoA);
            assertEquals("/data/repo-a.git", repoA.getDiskPath());
            assertEquals("main", repoA.getDefaultBranch());
        }
    }
}
