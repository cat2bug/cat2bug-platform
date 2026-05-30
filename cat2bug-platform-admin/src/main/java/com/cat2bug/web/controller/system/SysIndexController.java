package com.cat2bug.web.controller.system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 首页（无内嵌前端时的 Thymeleaf 欢迎页；内嵌模式下由 {@link com.cat2bug.framework.config.EmbeddedSpaFallbackFilter} 优先响应）
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
