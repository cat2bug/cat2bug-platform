package com.cat2bug;

import com.cat2bug.common.config.InstallStartupSupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportRuntimeHints;
import com.cat2bug.nativeimage.Cat2BugNativeRuntimeHints;
import com.cat2bug.nativeimage.JwtNativeRuntimeHints;
import com.cat2bug.nativeimage.MyBatisNativeConfiguration;
import com.cat2bug.nativeimage.SecurityNativeRuntimeHints;

import java.io.IOException;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@Import(MyBatisNativeConfiguration.class)
@ImportRuntimeHints({ Cat2BugNativeRuntimeHints.class, JwtNativeRuntimeHints.class, SecurityNativeRuntimeHints.class })
public class Cat2BugApplication
{
    public static void main(String[] args) throws InterruptedException, IOException {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        activateNativeProfileWhenRunningNativeImage();
        args = InstallStartupSupport.prepare(args);
        SpringApplication application = new SpringApplication(Cat2BugApplication.class);
        InstallStartupSupport.applyDefaultProperties(application);
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  Cat2Bug-Platform 启动成功   ლ(´ڡ`ლ)ﾞ ");
        Cat2BugApplication.drawColorLine();
    }

    /**
     * GraalVM Native 二进制运行时激活 native profile（加载 application-native.properties）。
     */
    private static void activateNativeProfileWhenRunningNativeImage()
    {
        if (System.getProperty("org.graalvm.nativeimage.imagecode") != null
                && System.getProperty("spring.profiles.active") == null)
        {
            System.setProperty("spring.profiles.active", "native");
        }
    }

    /**
     * 绘制彩条
     */
    private static void drawColorLine() {
        for (int i=0;i<1;i++) {
            for (int j=1;j<=15;j++) {
                int number = i * 15 + j;
                String str = String.format("%10s", " ");
                System.out.printf("\u001b[48;5;%dm %s",number,str);
                System.out.print("\u001b[0m");
            }
        }
        System.out.println();
    }
}
