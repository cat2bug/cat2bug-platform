package com.cat2bug;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class Cat2BugApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(Cat2BugApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  CatBug启动成功   ლ(´ڡ`ლ)ﾞ ");
    }
}
