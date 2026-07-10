package com.cat2bug.system.mapper;

import com.cat2bug.common.core.domain.entity.SysRole;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.system.domain.SysUserProject;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.DatabaseIdProvider;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 项目 not-member 候选池：全局用户减已入项目，不要求同团队成员。
 */
public class SysUserProjectMapperNotMemberTest {

    private static final long PROJECT_ID = 100L;

    private Connection connection;
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:project_not_member_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        connection = dataSource.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE sys_user ("
                    + "user_id BIGINT PRIMARY KEY, dept_id BIGINT, user_name VARCHAR(32), nick_name VARCHAR(32),"
                    + "email VARCHAR(64), avatar VARCHAR(255), phonenumber VARCHAR(32), password VARCHAR(64),"
                    + "sex CHAR(1), status CHAR(1), del_flag CHAR(1), login_ip VARCHAR(64), login_date TIMESTAMP,"
                    + "create_by VARCHAR(64), create_time TIMESTAMP, remark VARCHAR(255))");
            stmt.execute("CREATE TABLE sys_user_project ("
                    + "user_project_id BIGINT PRIMARY KEY, user_id BIGINT, project_id BIGINT, project_lock INT)");
            stmt.execute("INSERT INTO sys_user VALUES "
                    + "(1, NULL, 'team_user', 'Team User', NULL, NULL, NULL, NULL, '0', '0', '0', NULL, NULL, NULL, CURRENT_TIMESTAMP, NULL),"
                    + "(2, NULL, 'global_user', 'Global User', NULL, NULL, NULL, NULL, '0', '0', '0', NULL, NULL, NULL, CURRENT_TIMESTAMP, NULL),"
                    + "(3, NULL, 'in_project', 'In Project', NULL, NULL, NULL, NULL, '0', '0', '0', NULL, NULL, NULL, CURRENT_TIMESTAMP, NULL)");
            stmt.execute("INSERT INTO sys_user_project VALUES (1, 3, " + PROJECT_ID + ", 0)");
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("h2-not-member", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultScriptingLanguage(XMLLanguageDriver.class);
        configuration.getTypeAliasRegistry().registerAlias("SysUser", SysUser.class);
        configuration.getTypeAliasRegistry().registerAlias("SysUserProject", SysUserProject.class);
        configuration.getTypeAliasRegistry().registerAlias("SysRole", SysRole.class);
        configuration.addMapper(SysUserProjectMapper.class);

        DatabaseIdProvider databaseIdProvider = new org.apache.ibatis.mapping.VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("H2", "h2");
        ((org.apache.ibatis.mapping.VendorDatabaseIdProvider) databaseIdProvider).setProperties(properties);
        configuration.setDatabaseId("h2");

        loadMapper(configuration, "mapper/system/SysUserProjectMapper.xml");
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
    public void selectNotSysUserListByProjectId_includesGlobalUserNotInTeam() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserProjectMapper mapper = session.getMapper(SysUserProjectMapper.class);
            SysUser query = new SysUser();
            List<SysUser> users = mapper.selectNotSysUserListByProjectId(PROJECT_ID, query);
            List<Long> ids = users.stream().map(SysUser::getUserId).collect(Collectors.toList());
            assertTrue(ids.contains(2L));
            assertTrue(ids.contains(1L));
            assertTrue(!ids.contains(3L));
        }
    }

    @Test
    public void selectNotSysUserListByProjectId_searchFiltersByNickName() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysUserProjectMapper mapper = session.getMapper(SysUserProjectMapper.class);
            SysUser query = new SysUser();
            Map<String, Object> params = new HashMap<>();
            params.put("search", "Global");
            query.setParams(params);
            List<SysUser> users = mapper.selectNotSysUserListByProjectId(PROJECT_ID, query);
            assertEquals(1, users.size());
            assertEquals(Long.valueOf(2L), users.get(0).getUserId());
        }
    }
}
