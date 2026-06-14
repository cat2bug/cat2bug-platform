package com.cat2bug.web.captcha;

import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.utils.uuid.IdUtils;
import com.cat2bug.system.service.ISysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Native 验证码（无 AWT），使用 {@link CaptchaPngRenderer} 生成 PNG。
 */
@Profile("native")
@Component
public class NativeCaptchaSupport {

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    public AjaxResult createCaptchaImage() {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        ajax.put("registerEnabled", configService.selectRegisterEnabled());
        if (!captchaEnabled) {
            return ajax;
        }

        String code = randomCode(4);
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        redisCache.setCacheObject("verifyCode", verifyKey, code);

        ajax.put("uuid", uuid);
        ajax.put("captchaExpr", code);
        ajax.put("img", CaptchaPngRenderer.renderBase64Png(code));
        return ajax;
    }

    private static String randomCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
