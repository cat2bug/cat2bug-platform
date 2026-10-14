package com.cat2bug.quarkus.admin.task;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

/**
 * 定时任务调用目标示例：{@code ryTask.ryNoParams}、{@code ryTask.ryParams('x')}。
 * 用 {@code @Singleton} 避免客户端代理，任务按 bean 实际类反射调用方法。
 */
@Singleton
@Named("ryTask")
@Unremovable
@RegisterForReflection(methods = true)
public class RyTask {

    private static final Logger LOG = Logger.getLogger(RyTask.class);

    public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        LOG.infof("执行多参方法： 字符串类型%s，布尔类型%s，长整型%s，浮点型%s，整形%s", s, b, l, d, i);
    }

    public void ryParams(String params) {
        LOG.infof("执行有参方法：%s", params);
    }

    public void ryNoParams() {
        LOG.info("执行无参方法");
    }
}
