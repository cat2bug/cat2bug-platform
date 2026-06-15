package com.cat2bug.web.controller.common;

import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.web.captcha.jvm.JvmCaptchaSupport;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JVM 验证码接口（kaptcha + AWT）；Native profile 不编译此类。
 */
@Profile("!native")
@RestController
public class JvmCaptchaController {

    @Autowired
    private JvmCaptchaSupport jvmCaptchaSupport;

    @GetMapping("/captchaImage")
    public AjaxResult getCode(HttpServletResponse response) {
        return jvmCaptchaSupport.createCaptchaImage(response);
    }
}
