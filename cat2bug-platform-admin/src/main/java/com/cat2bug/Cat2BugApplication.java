package com.cat2bug;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class Cat2BugApplication
{
    public static void main(String[] args) throws InterruptedException, IOException {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(Cat2BugApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Cat2Bug-Platform 启动成功   ლ(´ڡ`ლ)ﾞ ");
        Cat2BugApplication.drawColorLine();
    }

    /**
     * 绘制彩条
     */
    private static void drawColorLine() {
        for (int i=0;i<1;i++) {
            for (int j=1;j<=15;j++) {
                int number = i * 15 + j;
                String str = String.format("%10s", " ");
                System.out.print(String.format("\u001b[48;5;%dm %s",number,str));
                System.out.print("\u001b[0m");
            }
        }
    }
}
