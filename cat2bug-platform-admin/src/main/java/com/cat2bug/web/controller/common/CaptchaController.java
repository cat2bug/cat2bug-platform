package com.cat2bug.web.controller.common;

import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.web.captcha.CaptchaSupport;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证码操作处理
 */
@RestController
public class CaptchaController {

    @Autowired
    private CaptchaSupport captchaSupport;

    @GetMapping("/captchaImage")
    public AjaxResult getCode(HttpServletResponse response) {
        return captchaSupport.createCaptchaImage(response);
    }
}
