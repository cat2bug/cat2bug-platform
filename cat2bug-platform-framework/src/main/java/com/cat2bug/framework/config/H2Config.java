package com.cat2bug.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-21 03:52
 * @Version: 1.0.0
 */
@Slf4j
@Component
public class H2Config {
    private final JdbcTemplate jdbcTemplate;

    private final Resource sqlScript;

    public H2Config(JdbcTemplate jdbcTemplate,
                    @Value("${spring.database-type}") String databaseType,
                    @Value("classpath:h2-schema.sql") Resource sqlScript) throws Exception {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlScript = sqlScript;
        if ("h2".equals(databaseType)) {
            try {
                if(isDatabaseEmpty()) {
                    initData();
                }
            } catch (Exception e) {
                initData();
            }
        }
    }

    private boolean isDatabaseEmpty() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class) == 0;
    }

    private void initData() throws Exception {
        try (Reader reader = new InputStreamReader(sqlScript.getInputStream(), StandardCharsets.UTF_8)) {
            String sql = FileCopyUtils.copyToString(reader);
            jdbcTemplate.execute(sql);
        }
    }
}
