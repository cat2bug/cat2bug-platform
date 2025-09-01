package com.cat2bug.web.controller.system;

import com.cat2bug.common.config.Cat2BugConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 首页
 *
 * @author ruoyi
 */
@Controller
public class SysIndexController
{
    /**
     * 访问首页，提示语
     */
    @RequestMapping(value = {"/","/index"})
    public String index()
    {
        return "index";
    }
}
