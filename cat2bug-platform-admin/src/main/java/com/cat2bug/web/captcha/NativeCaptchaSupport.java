package com.cat2bug.web.captcha;

import com.cat2bug.common.config.Cat2BugConfig;
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
    private static final String MATH_OPS = "+-*";

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

        String captchaType = Cat2BugConfig.getCaptchaType();
        String code;
        String capStr;
        if ("math".equals(captchaType)) {
            capStr = randomMathExpression();
            code = String.valueOf(evalMathExpression(capStr));
        } else {
            code = randomCode(4);
            capStr = code;
        }

        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        redisCache.setCacheObject("verifyCode", verifyKey, code);

        ajax.put("uuid", uuid);
        ajax.put("captchaExpr", capStr);
        ajax.put("img", CaptchaPngRenderer.renderBase64Png(capStr));
        return ajax;
    }

    private static String randomMathExpression() {
        SecureRandom random = new SecureRandom();
        int x = random.nextInt(10);
        int y = random.nextInt(10);
        char op = MATH_OPS.charAt(random.nextInt(MATH_OPS.length()));
        if (op == '-' && x < y) {
            int tmp = x;
            x = y;
            y = tmp;
        }
        if (op == '/' && y == 0) {
            y = 1;
        }
        return x + String.valueOf(op) + y + "=?";
    }

    private static int evalMathExpression(String expression) {
        String expr = expression.replace("=?", "").trim();
        char op = expr.charAt(expr.length() - 2);
        int x = Character.getNumericValue(expr.charAt(0));
        int y = Character.getNumericValue(expr.charAt(2));
        return switch (op) {
            case '+' -> x + y;
            case '-' -> x - y;
            case '*' -> x * y;
            case '/' -> y == 0 ? 0 : x / y;
            default -> 0;
        };
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
