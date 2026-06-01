package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.SysDefectOpenWorkload;
import com.cat2bug.system.domain.SysDefectOpenWorkloadSummary;
import com.cat2bug.system.mapper.SysDefectStatisticMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 未关闭待办负载：Service 委托与空结果兜底。
 */
public class SysDefectStatisticServiceOpenWorkloadTest {

    @Mock
    private SysDefectStatisticMapper sysDefectStatisticMapper;

    @InjectMocks
    private SysDefectStatisticServiceImpl sysDefectStatisticService;

    private Connection h2Connection;
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:defect_statistic_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        h2Connection = dataSource.getConnection();
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS sys_defect");
            stmt.execute("DROP TABLE IF EXISTS sys_user_project");
            stmt.execute("DROP TABLE IF EXISTS sys_user");
            stmt.execute("CREATE TABLE sys_user (user_id BIGINT PRIMARY KEY, nick_name VARCHAR(32), avatar VARCHAR(255), del_flag CHAR(1))");
            stmt.execute("CREATE TABLE sys_user_project (user_project_id BIGINT PRIMARY KEY, user_id BIGINT, project_id BIGINT)");
            stmt.execute("CREATE TABLE sys_defect (defect_id BIGINT PRIMARY KEY, project_id BIGINT, del_flag CHAR(1), defect_state INT, handle_by VARCHAR(255))");
            stmt.execute("INSERT INTO sys_user(user_id, nick_name, avatar, del_flag) VALUES (1, 'u1', 'a1', '0')");
            stmt.execute("INSERT INTO sys_user(user_id, nick_name, avatar, del_flag) VALUES (2, 'u2', 'a2', '0')");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (11, 1, 100)");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (12, 2, 100)");
            stmt.execute("INSERT INTO sys_user(user_id, nick_name, avatar, del_flag) VALUES (3, 'u3', 'a3', '0')");
            stmt.execute("INSERT INTO sys_user_project(user_project_id, user_id, project_id) VALUES (13, 3, 100)");
            stmt.execute("INSERT INTO sys_defect(defect_id, project_id, del_flag, defect_state, handle_by) VALUES (101, 100, '0', 0, '[1,2]')");
            stmt.execute("INSERT INTO sys_defect(defect_id, project_id, del_flag, defect_state, handle_by) VALUES (102, 100, '0', 3, '[1]')");
            stmt.execute("INSERT INTO sys_defect(defect_id, project_id, del_flag, defect_state, handle_by) VALUES (103, 100, '0', 4, '[1,2]')");
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("h2test", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultScriptingLanguage(XMLLanguageDriver.class);
        configuration.addMapper(SysDefectStatisticMapper.class);

        DatabaseIdProvider databaseIdProvider = new org.apache.ibatis.mapping.VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("H2", "h2");
        ((org.apache.ibatis.mapping.VendorDatabaseIdProvider) databaseIdProvider).setProperties(properties);
        configuration.setDatabaseId(databaseIdProvider.getDatabaseId(dataSource));

        String mapperResource = "mapper/system/SysDefectStatisticMapper.xml";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(mapperResource)) {
            assertNotNull(inputStream);
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    mapperResource,
                    configuration.getSqlFragments()
            );
            xmlMapperBuilder.parse();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @After
    public void tearDown() throws Exception {
        if (h2Connection != null) {
            h2Connection.close();
        }
    }

    @Test
    public void openWorkload_delegatesToMapper() {
        SysDefectOpenWorkload row = new SysDefectOpenWorkload();
        row.setUserId(1L);
        row.setTotal(3);
        when(sysDefectStatisticMapper.openWorkloadByProject(10L)).thenReturn(Collections.singletonList(row));

        List<SysDefectOpenWorkload> list = sysDefectStatisticService.openWorkload(10L);

        assertEquals(1, list.size());
        assertEquals(3, list.get(0).getTotal());
        verify(sysDefectStatisticMapper).openWorkloadByProject(eq(10L));
    }

    @Test
    public void openWorkloadMy_returnsZeroSummaryWhenMapperNull() {
        when(sysDefectStatisticMapper.openWorkloadByMember(10L, 2L)).thenReturn(null);

        SysDefectOpenWorkloadSummary summary = sysDefectStatisticService.openWorkloadMy(10L, 2L);

        assertNotNull(summary);
        assertEquals(0, summary.getTotal());
    }

    @Test
    public void openWorkloadMy_passesThroughMapperResult() {
        SysDefectOpenWorkloadSummary expected = new SysDefectOpenWorkloadSummary();
        expected.setTotal(4);
        expected.setProcessing(2);
        expected.setAudit(1);
        expected.setRejected(1);
        when(sysDefectStatisticMapper.openWorkloadByMember(10L, 2L)).thenReturn(expected);

        SysDefectOpenWorkloadSummary summary = sysDefectStatisticService.openWorkloadMy(10L, 2L);

        assertEquals(4, summary.getTotal());
        assertEquals(2, summary.getProcessing());
    }

    @Test
    public void h2MapperQuery_shouldDoubleCountMultiHandler_andExcludeClosed() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            SysDefectStatisticMapper mapper = sqlSession.getMapper(SysDefectStatisticMapper.class);
            List<SysDefectOpenWorkload> workloads = mapper.openWorkloadByProject(100L);
            assertEquals(3, workloads.size());

            SysDefectOpenWorkload user1 = workloads.stream().filter(w -> Long.valueOf(1L).equals(w.getUserId())).findFirst().orElse(null);
            SysDefectOpenWorkload user2 = workloads.stream().filter(w -> Long.valueOf(2L).equals(w.getUserId())).findFirst().orElse(null);
            SysDefectOpenWorkload user3 = workloads.stream().filter(w -> Long.valueOf(3L).equals(w.getUserId())).findFirst().orElse(null);
            assertNotNull(user1);
            assertNotNull(user2);
            assertNotNull(user3);
            assertEquals(2, user1.getTotal());
            assertEquals(1, user2.getTotal());
            assertEquals(0, user3.getTotal());

            SysDefectOpenWorkloadSummary my = mapper.openWorkloadByMember(100L, 1L);
            assertNotNull(my);
            assertEquals(2, my.getTotal());
            assertEquals(1, my.getProcessing());
            assertEquals(0, my.getAudit());
            assertEquals(1, my.getRejected());
        }
    }
}
