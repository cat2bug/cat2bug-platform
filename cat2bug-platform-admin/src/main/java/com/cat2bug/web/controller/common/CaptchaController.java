package com.cat2bug.web.controller.common;

import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.web.captcha.NativeCaptchaSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Native 验证码接口（无 kaptcha/AWT）。
 */
@Profile("native")
@RestController
public class CaptchaController {

    @Autowired
    private NativeCaptchaSupport nativeCaptchaSupport;

    @GetMapping("/captchaImage")
    public AjaxResult getCode() {
        return nativeCaptchaSupport.createCaptchaImage();
    }
}
