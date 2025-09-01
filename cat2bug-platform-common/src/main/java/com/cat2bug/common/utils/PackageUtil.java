package com.cat2bug.common.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-09-13 12:37
 * @Version: 1.0.0
 * 包工具
 */
@Component
public class PackageUtil {
    private final static Logger log = LogManager.getLogger(PackageUtil.class);

    /**
     * 获取所有已经设置的扫包路径
     * @return  包路径集合
     */
    public String[] getAllScanPackage() {
        Set<Class<?>> componentScans  = AnnotationUtils.getClassesWithAnnotation("com.cat2bug",ComponentScan.class);
        Set<String> ret = new HashSet<>();
        componentScans.stream().forEach(c->{
            ComponentScan componentScan = c.getAnnotation(ComponentScan.class);
            if(componentScan==null) {
                ret.add(c.getPackage().getName());
            } else if(componentScan.basePackages()!=null) {
                ret.addAll(Arrays.asList(componentScan.basePackages()));
            }
        });
        return ret.toArray(new String[]{});
    }
}
