package com.cat2bug.system.mapper;

import com.cat2bug.common.core.domain.entity.SysDefect;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * H2 集成测试：customFieldFilters JSON 筛选（eq / contains / isEmpty）。
 */
public class SysDefectCustomFieldFilterMapperTest {

    private Connection h2Connection;
    private SqlSessionFactory sqlSessionFactory;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:defect_custom_field_filter_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        h2Connection = dataSource.getConnection();
        try (Statement stmt = h2Connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS sys_defect_log");
            stmt.execute("DROP TABLE IF EXISTS sys_user_defect");
            stmt.execute("DROP TABLE IF EXISTS sys_user");
            stmt.execute("DROP TABLE IF EXISTS sys_module");
            stmt.execute("DROP TABLE IF EXISTS sys_project");
            stmt.execute("DROP TABLE IF EXISTS sys_defect");
            stmt.execute("CREATE TABLE sys_project (project_id BIGINT PRIMARY KEY, project_name VARCHAR(64))");
            stmt.execute("CREATE TABLE sys_module (module_id BIGINT PRIMARY KEY, module_name VARCHAR(64))");
            stmt.execute("CREATE TABLE sys_user (user_id BIGINT PRIMARY KEY, nick_name VARCHAR(32), user_name VARCHAR(32), avatar VARCHAR(255), del_flag CHAR(1))");
            stmt.execute("CREATE TABLE sys_user_defect (defect_id BIGINT, user_id BIGINT, collect INT)");
            stmt.execute("CREATE TABLE sys_defect_log (log_id BIGINT PRIMARY KEY, defect_id BIGINT, defect_log_type INT, create_by BIGINT, create_time TIMESTAMP)");
            stmt.execute(
                    "CREATE TABLE sys_defect ("
                            + "defect_id BIGINT PRIMARY KEY, project_id BIGINT, project_num BIGINT, del_flag CHAR(1), "
                            + "defect_name VARCHAR(128), defect_type INT, defect_state INT, handle_by VARCHAR(255), "
                            + "custom_fields JSON, create_by_id BIGINT, handle_time TIMESTAMP, defect_level VARCHAR(32), "
                            + "plan_start_time TIMESTAMP, plan_end_time TIMESTAMP, sponsor VARCHAR(64), "
                            + "module_id BIGINT, module_version VARCHAR(64), update_time TIMESTAMP, create_time TIMESTAMP"
                            + ")"
            );
            stmt.execute("INSERT INTO sys_project(project_id, project_name) VALUES (100, 'test')");
            stmt.execute(
                    "INSERT INTO sys_defect(defect_id, project_id, project_num, del_flag, defect_name, custom_fields) "
                            + "VALUES (1, 100, 1, '0', 'prod-defect', '{\"env\":\"prod\",\"severity\":\"P0\"}')"
            );
            stmt.execute(
                    "INSERT INTO sys_defect(defect_id, project_id, project_num, del_flag, defect_name, custom_fields) "
                            + "VALUES (2, 100, 2, '0', 'staging-defect', '{\"env\":\"staging\"}')"
            );
            stmt.execute(
                    "INSERT INTO sys_defect(defect_id, project_id, project_num, del_flag, defect_name, custom_fields) "
                            + "VALUES (3, 100, 3, '0', 'empty-custom', NULL)"
            );
        }

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("h2test", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setDefaultScriptingLanguage(XMLLanguageDriver.class);
        configuration.addMapper(SysDefectMapper.class);

        DatabaseIdProvider databaseIdProvider = new org.apache.ibatis.mapping.VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("H2", "h2");
        ((org.apache.ibatis.mapping.VendorDatabaseIdProvider) databaseIdProvider).setProperties(properties);
        configuration.setDatabaseId(databaseIdProvider.getDatabaseId(dataSource));

        String mapperResource = "mapper/system/SysDefectMapper.xml";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(mapperResource)) {
            assertNotNull(mapperResource + " not on classpath", inputStream);
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
    public void h2_customFieldFilters_eq() {
        List<Long> ids = queryDefectIds(filter("env", "eq", "prod"));
        assertEquals(1, ids.size());
        assertEquals(Long.valueOf(1L), ids.get(0));
    }

    @Test
    public void h2_customFieldFilters_contains() {
        List<Long> ids = queryDefectIds(filter("env", "contains", "prod"));
        assertEquals(1, ids.size());
        assertEquals(Long.valueOf(1L), ids.get(0));
    }

    @Test
    public void h2_customFieldFilters_isEmpty() {
        List<Long> ids = queryDefectIds(filter("env", "isEmpty", null));
        assertTrue(ids.contains(3L));
        assertTrue(!ids.contains(1L));
        assertTrue(!ids.contains(2L));
    }

    private static Map<String, Object> filter(String fieldKey, String op, Object value) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("fieldKey", fieldKey);
        f.put("op", op);
        if (value != null) {
            f.put("value", value);
        }
        return f;
    }

    private List<Long> queryDefectIds(Map<String, Object> filterEntry) {
        SysDefect query = new SysDefect();
        query.setProjectId(100L);
        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(filterEntry);
        Map<String, Object> params = new HashMap<>();
        params.put("customFieldFilters", filters);
        query.setParams(params);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            SysDefectMapper mapper = session.getMapper(SysDefectMapper.class);
            List<SysDefect> list = mapper.selectSysDefectList(query, null, new Date());
            return list.stream().map(SysDefect::getDefectId).collect(Collectors.toList());
        }
    }
}
