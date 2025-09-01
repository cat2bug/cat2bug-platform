package com.cat2bug.common.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-09-11 16:00
 * @Version: 1.0.0
 * 类操作
 */
public class ClassUtils {
    private final static Logger log = LogManager.getLogger(ClassUtils.class);
    /**
     * 默认过滤器（无实现）
     */
    private final static Predicate<Class<?>> EMPTY_FILTER = clazz -> true;

    /**
     * 判断指定类是否继承了父类
     * @param clazz        目标类
     * @param superClass    父类
     * @return  是否
     */
    public static boolean isSubclassOf(Class<?> clazz, Class<?> superClass) {
        if (clazz == null || superClass == null) {
            return false;
        }
        if (clazz.equals(superClass)) {
            return true;
        }
        Class<?> parent = clazz.getSuperclass();
        if (parent == null) {
            return false;
        }
        return isSubclassOf(parent, superClass);
    }

    /**
     * 扫描目录下的所有class文件
     *
     * @param scanPackage 搜索的包根路径
     * @return
     */
    public static Set<Class<?>> getClasses(String scanPackage) {
        return getClasses(scanPackage, EMPTY_FILTER);
    }

    /**
     * 返回所有的子类（不包括抽象类）
     *
     * @param scanPackage 搜索的包根路径
     * @param parent
     * @return
     */
    public static Set<Class<?>> listAllSubclasses(String scanPackage, Class<?> parent) {
        return getClasses(scanPackage, (clazz) -> {
            return parent.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers());
        });
    }

    /**
     * 返回所有带制定注解的class列表
     *
     * @param scanPackage 搜索的包根路径
     * @param annotation
     * @return
     */
    public static <A extends Annotation> Set<Class<?>> listClassesWithAnnotation(String scanPackage,
                                                                                 Class<A> annotation) {
        return getClasses(scanPackage, (clazz) -> {
            return clazz.getAnnotation(annotation) != null;
        });
    }

    /**
     * 扫描目录下的所有class文件
     *
     * @param pack   包路径
     * @param filter 自定义类过滤器
     * @return
     */
    public static Set<Class<?>> getClasses(String pack, Predicate<Class<?>> filter) {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metaFactory = new SimpleMetadataReaderFactory(patternResolver);

        String path = org. springframework.util.ClassUtils.convertClassNameToResourcePath(pack);
//        String location = ResourceUtils.CLASSPATH_URL_PREFIX + path + "/**/*.class";
        String location = "classpath*:" + path + "/**/*.class";
        Resource[] resources;

        Set<Class<?>> result = new HashSet<>();
        try {
            resources = patternResolver.getResources(location);
            for (Resource resource : resources) {
                MetadataReader metaReader = metaFactory.getMetadataReader(resource);
                if (resource.isReadable()) {
                    String clazzName = metaReader.getClassMetadata().getClassName();
                    if (clazzName.contains("$")) {
                        // 忽略内部类
                        continue;
                    }
//					Class<?> clazz = Class.forName(clazzName);
                    Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(clazzName);
                    if (filter.test(clazz)) {
                        result.add(clazz);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }

        return result;
    }

    public static void main(String[] args) {
        Set<Class<?>> clazzs = getClasses("com.stringbj");
        System.err.println(clazzs);

        Set<Class<?>> clazzs2 = listAllSubclasses("com.stringbj", ClassUtils.class);
        System.err.println(clazzs2);
    }
}
