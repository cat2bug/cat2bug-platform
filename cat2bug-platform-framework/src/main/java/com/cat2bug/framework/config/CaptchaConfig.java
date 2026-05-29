package com.cat2bug.framework.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import static com.google.code.kaptcha.Constants.*;

/**
 * 验证码配置
 * 
 * @author ruoyi
 */
@Configuration
public class CaptchaConfig
{
    /** 登录表单描边色 #5A5A59，与 cat2bug-logo 插画线稿一致 */
    private static final String BRAND_BORDER = "90,90,89";
    /** 深灰字符，贴近 logo 描边色 */
    private static final String BRAND_TEXT = "74,74,73";
    /** 暖白 → 柔黄背景，呼应 logo 圆形光晕 */
    private static final String BRAND_BG_FROM = "255,252,245";
    private static final String BRAND_BG_TO = "255,243,214";
    /** 浅灰干扰，避免喧宾夺主 */
    private static final String BRAND_NOISE = "190,188,184";

    private static final String CAPTCHA_WIDTH = "150";
    private static final String CAPTCHA_HEIGHT = "38";

    private static void applyBrandStyle(Properties properties, String fontSize, String charLength, String charSpace)
    {
        properties.setProperty(KAPTCHA_BORDER, "yes");
        properties.setProperty(KAPTCHA_BORDER_COLOR, BRAND_BORDER);
        properties.setProperty(KAPTCHA_BORDER_THICKNESS, "2");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, BRAND_TEXT);
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, CAPTCHA_WIDTH);
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, CAPTCHA_HEIGHT);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, fontSize);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, charLength);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_SPACE, charSpace);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Arial");
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_FROM, BRAND_BG_FROM);
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_TO, BRAND_BG_TO);
        properties.setProperty(KAPTCHA_BACKGROUND_IMPL, "com.cat2bug.framework.config.Cat2BugKaptchaBackground");
        properties.setProperty(KAPTCHA_NOISE_COLOR, BRAND_NOISE);
        properties.setProperty(KAPTCHA_NOISE_IMPL, "com.cat2bug.framework.config.Cat2BugKaptchaNoise");
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.google.code.kaptcha.impl.WaterRipple");
    }

    @Bean(name = "captchaProducer")
    public DefaultKaptcha getKaptchaBean()
    {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        applyBrandStyle(properties, "30", "4", "4");
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "kaptchaCode");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }

    @Bean(name = "captchaProducerMath")
    public DefaultKaptcha getKaptchaBeanMath()
    {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        applyBrandStyle(properties, "28", "6", "3");
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "kaptchaCodeMath");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_IMPL, "com.cat2bug.framework.config.KaptchaTextCreator");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
