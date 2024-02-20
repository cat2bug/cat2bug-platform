package com.cat2bug.web.controller.system;

import com.cat2bug.common.config.Cat2BugConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-18 17:32
 * @Version: 1.0.0
 * 版本
 */
@RestController
public class SysVersionController {
    /** 系统基础配置 */
    @Autowired
    private Cat2BugConfig cat2bugConfig;

    @RequestMapping("/version")
    public String version()
    {
        return cat2bugConfig.getVersion();
    }
}
