package com.cat2bug.web.captcha;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.common.core.domain.AjaxResult;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.utils.NativeImageSupport;
import com.cat2bug.common.utils.sign.Base64;
import com.cat2bug.common.utils.uuid.IdUtils;
import com.cat2bug.system.service.ISysConfigService;
import com.google.code.kaptcha.Producer;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * 验证码生成（JVM 用 kaptcha+AWT，Native 用 {@link CaptchaPngRenderer}，不依赖 @Profile AOT 裁剪）。
 */
@Component
public class CaptchaSupport
{
    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final String MATH_OPS = "+-*";

    @Autowired
    @Qualifier("captchaProducer")
    private ObjectProvider<Producer> captchaProducer;

    @Autowired
    @Qualifier("captchaProducerMath")
    private ObjectProvider<Producer> captchaProducerMath;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysConfigService configService;

    public AjaxResult createCaptchaImage(HttpServletResponse response)
    {
        if (NativeImageSupport.isRunningNativeImage() || !hasJvmCaptchaProducers())
        {
            return createNativeCaptchaImage();
        }
        return createJvmCaptchaImage(response);
    }

    private boolean hasJvmCaptchaProducers()
    {
        return captchaProducer.getIfAvailable() != null
                && captchaProducerMath.getIfAvailable() != null;
    }

    private AjaxResult createNativeCaptchaImage()
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        ajax.put("registerEnabled", configService.selectRegisterEnabled());
        if (!captchaEnabled)
        {
            return ajax;
        }

        String captchaType = Cat2BugConfig.getCaptchaType();
        String code;
        String capStr;
        if ("math".equals(captchaType))
        {
            capStr = randomMathExpression();
            code = String.valueOf(evalMathExpression(capStr));
        }
        else
        {
            code = randomCode(4);
            capStr = code;
        }

        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        storeVerifyCode(verifyKey, code);

        ajax.put("uuid", uuid);
        ajax.put("captchaExpr", capStr);
        ajax.put("img", CaptchaPngRenderer.renderBase64Png(capStr));
        return ajax;
    }

    private static String randomMathExpression()
    {
        SecureRandom random = new SecureRandom();
        int x = random.nextInt(10);
        int y = random.nextInt(10);
        char op = MATH_OPS.charAt(random.nextInt(MATH_OPS.length()));
        if (op == '-' && x < y)
        {
            int tmp = x;
            x = y;
            y = tmp;
        }
        if (op == '/' && y == 0)
        {
            y = 1;
        }
        return x + String.valueOf(op) + y + "=?";
    }

    private static int evalMathExpression(String expression)
    {
        String expr = expression.replace("=?", "").trim();
        char op = expr.charAt(expr.length() - 2);
        int x = Character.getNumericValue(expr.charAt(0));
        int y = Character.getNumericValue(expr.charAt(2));
        return switch (op)
        {
            case '+' -> x + y;
            case '-' -> x - y;
            case '*' -> x * y;
            case '/' -> y == 0 ? 0 : x / y;
            default -> 0;
        };
    }

    private AjaxResult createJvmCaptchaImage(HttpServletResponse response)
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        ajax.put("registerEnabled", configService.selectRegisterEnabled());
        if (!captchaEnabled)
        {
            return ajax;
        }

        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        Producer charProducer = captchaProducer.getIfAvailable();
        Producer mathProducer = captchaProducerMath.getIfAvailable();
        if (charProducer == null || mathProducer == null)
        {
            return createNativeCaptchaImage();
        }

        String capStr = null;
        String code = null;
        BufferedImage image = null;

        String captchaType = Cat2BugConfig.getCaptchaType();
        if ("math".equals(captchaType))
        {
            String capText = mathProducer.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = mathProducer.createImage(capStr);
        }
        else if ("char".equals(captchaType))
        {
            capStr = code = charProducer.createText();
            image = charProducer.createImage(capStr);
        }
        else
        {
            return createNativeCaptchaImage();
        }

        storeVerifyCode(verifyKey, code);
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "png", os);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }

        ajax.put("uuid", uuid);
        ajax.put("captchaExpr", capStr);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }

    private void storeVerifyCode(String verifyKey, String code)
    {
        redisCache.setCacheObject("verifyCode", verifyKey, code);
    }

    private static String randomCode(int length)
    {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
        {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
