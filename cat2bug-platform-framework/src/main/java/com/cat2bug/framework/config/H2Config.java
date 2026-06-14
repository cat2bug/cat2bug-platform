package com.cat2bug.framework.config;

import com.cat2bug.framework.service.InstallService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import jakarta.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * H2 空库灌入（legacy）。未安装时不灌入带 seed admin 的数据，由安装向导 + Flyway 初始化。
 *
 * @Author: yuzhantao
 * @CreateTime: 2024-02-21 03:52
 * @Version: 1.0.0
 */
@Slf4j
@Component
public class H2Config
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Lazy
    @Autowired
    private InstallService installService;

    @Value("${spring.database-type}")
    private String databaseType;

    @Value("classpath:h2-schema.sql")
    private Resource sqlScript;

    @PostConstruct
    public void initH2IfNeeded() throws Exception
    {
        if (!"h2".equals(databaseType))
        {
            return;
        }
        if (!installService.isInstalled())
        {
            log.info("系统未安装，跳过 h2-schema.sql 灌入，请通过安装向导完成初始化");
            return;
        }
        try
        {
            if (isDatabaseEmpty())
            {
                initData();
            }
        }
        catch (Exception e)
        {
            log.warn("H2 空库检测失败: {}", e.getMessage());
            if (sqlScript.exists())
            {
                log.info("尝试 legacy h2-schema.sql 灌入");
                initData();
            }
        }
    }

    private boolean isDatabaseEmpty()
    {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class) == 0;
    }

    private void initData() throws Exception
    {
        try (Reader reader = new InputStreamReader(sqlScript.getInputStream(), StandardCharsets.UTF_8))
        {
            String sql = FileCopyUtils.copyToString(reader);
            jdbcTemplate.execute(sql);
        }
    }
}
