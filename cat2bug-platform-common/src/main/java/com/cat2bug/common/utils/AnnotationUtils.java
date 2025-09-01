package com.cat2bug.common.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-09-10 17:02
 * @Version: 1.0.0
 * 注解工具类
 */
public class AnnotationUtils {
    private final static Logger log = LogManager.getLogger(AnnotationUtils.class);
    /**
     * 获取标记了指定注解的所有类
     * @param packageName       包路径
     * @param annotationClass   注解类
     * @return                  类集合
     */
    public static Set<Class<?>> getClassesWithAnnotation(String packageName, Class<? extends Annotation> annotationClass) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(annotationClass));
        Set<BeanDefinition> candidates = provider.findCandidateComponents(packageName);

        Set<Class<?>> classes = new HashSet<>();
        for (BeanDefinition candidate : candidates) {
            try {
                Class<?> clazz = Class.forName(candidate.getBeanClassName());
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                log.error(e);
            }
        }

        return classes;
    }

}
