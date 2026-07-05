package com.cat2bug.framework.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MyBatisConfigDatabaseIdTest
{
    @Test
    void inferDatabaseTypeFromJdbcUrl() throws Exception
    {
        Method method = MyBatisConfig.class.getDeclaredMethod("inferDatabaseTypeFromJdbcUrl", String.class);
        method.setAccessible(true);

        assertEquals("mysql", method.invoke(null, "jdbc:mysql://127.0.0.1:3306/cat2bug_platform"));
        assertEquals("mysql", method.invoke(null, "jdbc:mariadb://127.0.0.1:3306/cat2bug_platform"));
        assertEquals("h2", method.invoke(null, "jdbc:h2:file:./data/cat2bug_platform;MODE=MySQL"));
        assertNull(method.invoke(null, "jdbc:postgresql://localhost/db"));
        assertNull(method.invoke(null, ""));
    }
}
