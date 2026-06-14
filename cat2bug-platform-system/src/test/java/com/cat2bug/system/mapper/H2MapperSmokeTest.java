package com.cat2bug.system.mapper;

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
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

/**
 * H2 下 MyBatis databaseId 分支冒烟：不启 Spring、不打 JAR，秒级验证 Mapper SQL。
 */
public class H2MapperSmokeTest {

    private Connection connection;
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:h2_mapper_smoke_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        connection = dataSource.getConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE sys_user (user_id BIGINT PRIMARY KEY, nick_name VARCHAR(32), user_name VARCHAR(32), avatar VARCHAR(255), del_flag CHAR(1))");
            stmt.execute("CREATE TABLE sys_user_project (user_project_id BIGINT PRIMARY KEY, user_id BIGINT, project_id BIGINT)");
            stmt.execute("CREATE TABLE sys_defect (defect_id BIGINT PRIMARY KEY, project_id BIGINT, del_flag CHAR(1), defect_state INT, handle_by VARCHAR(255), update_time TIMESTAMP)");
            stmt.execute("CREATE TABLE sys_defect_log (log_id BIGINT PRIMARY KEY, defect_id BIGINT, create_by BIGINT, create_time TIMESTAMP)");
            stmt.execute("INSERT INTO sys_user(user_id, nick_name, user_name, avatar, del_flag) VALUES (1, 'u1', 'u1', '', '0')");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (1, 1, 100)");
            stmt.execute("INSERT INTO sys_defect(defect_id, project_id, del_flag, defect_state, handle_by, update_time) VALUES (101, 100, '0', 0, '[1]', CURRENT_TIMESTAMP)");
            stmt.execute("INSERT INTO sys_defect_log(log_id, defect_id, create_by, create_time) VALUES (1, 101, 1, CURRENT_TIMESTAMP)");
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("h2-smoke", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultScriptingLanguage(XMLLanguageDriver.class);
        configuration.getTypeAliasRegistry().registerAliases("com.cat2bug.system.domain");
        configuration.addMapper(SysDefectStatisticMapper.class);

        DatabaseIdProvider databaseIdProvider = new org.apache.ibatis.mapping.VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("H2", "h2");
        ((org.apache.ibatis.mapping.VendorDatabaseIdProvider) databaseIdProvider).setProperties(properties);
        configuration.setDatabaseId("h2");

        loadMapper(configuration, "mapper/system/SysDefectStatisticMapper.xml");
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
    public void defectStatistic_openWorkload_usesH2Sql() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysDefectStatisticMapper mapper = session.getMapper(SysDefectStatisticMapper.class);
            assertNotNull(mapper.openWorkloadByProject(100L));
            assertNotNull(mapper.openWorkloadByMember(100L, 1L));
        }
    }

    @Test
    public void defectStatistic_stateStatistic_usesH2Sql() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysDefectStatisticMapper mapper = session.getMapper(SysDefectStatisticMapper.class);
            mapper.stateStatistic(100L, null);
        }
    }
}
