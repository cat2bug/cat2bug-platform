package com.cat2bug.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.utils.StringUtils;

/**
 * 首页
 *
 * @author ruoyi
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private Cat2BugConfig cat2bugConfig;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", cat2bugConfig.getName(), cat2bugConfig.getVersion());
    }

    @RequestMapping("/version")
    public String version()
    {
        return cat2bugConfig.getVersion();
    }
}
