package com.cat2bug.framework.security.filter;

import com.cat2bug.common.utils.ClassUtils;
import com.cat2bug.common.utils.PackageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-09-18 02:28
 * @Version: 1.0.0
 */
@Component
public class AuthenticationTokenFilterService {
    /**
     * 所有事项配置规则模版配置集合
     */
    private static List<AbstractCat2BugAuthenticationProcessingFilter> filterList = new ArrayList<>();

    @Autowired
    private PackageUtil packageUtil;

    @PostConstruct
    public void init() {
        String[] packages = packageUtil.getAllScanPackage();
        for(String packagePath : packages) {
            filterList.addAll(ClassUtils.listAllSubclasses(packagePath, AbstractCat2BugAuthenticationProcessingFilter.class).
                stream().map(c -> {
                    // 过滤模版Code为空的模版
                    try {
                        return (AbstractCat2BugAuthenticationProcessingFilter) c.newInstance();
                    } catch (Exception e) {
                        return null;
                    }
                }).collect(Collectors.toList()));
        }
    }

    /**
     * 获取所有验证过滤类
     * @return  过滤类集合
     */
    public static List<AbstractCat2BugAuthenticationProcessingFilter> getAllAuthenticationFilterList() {
        return AuthenticationTokenFilterService.filterList;
    }
}
