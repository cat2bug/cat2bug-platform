package com.cat2bug.web.captcha.jvm;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.utils.sign.Base64;
import com.cat2bug.common.utils.uuid.IdUtils;
import com.cat2bug.system.service.ISysConfigService;
import com.google.code.kaptcha.Producer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * JVM 验证码（kaptcha + AWT），Native profile 不编译/不加载。
 */
@Profile("!native")
@Component
public class JvmCaptchaSupport {

    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    public AjaxResult createCaptchaImage(HttpServletResponse response) throws RuntimeException {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        ajax.put("registerEnabled", configService.selectRegisterEnabled());
        if (!captchaEnabled) {
            return ajax;
        }

        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null;
        String code = null;
        BufferedImage image = null;

        String captchaType = Cat2BugConfig.getCaptchaType();
        if ("math".equals(captchaType)) {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        } else if ("char".equals(captchaType)) {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisCache.setCacheObject("verifyCode", verifyKey, code);
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        ajax.put("uuid", uuid);
        ajax.put("captchaExpr", capStr);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }
}
