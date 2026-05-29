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
    /** 插画线稿/表单描边 #5A5A59 */
    private static final String PIXEL_BORDER = Cat2BugKaptchaPalette.INK_RGB;
    /** 验证码文字 #5A5A59 */
    private static final String PIXEL_TEXT = Cat2BugKaptchaPalette.INK_RGB;
    /** 太阳光晕亮部 #FFF3D6 */
    private static final String PIXEL_BG_FROM = Cat2BugKaptchaPalette.SUN_LIGHT_RGB;
    /** 太阳暗部 #FFC145 */
    private static final String PIXEL_BG_TO = Cat2BugKaptchaPalette.SUN_DEEP_RGB;
    /** 折线干扰 #5A5A59 */
    private static final String PIXEL_NOISE = Cat2BugKaptchaPalette.INK_RGB;

    private static final String CAPTCHA_WIDTH = "150";
    private static final String CAPTCHA_HEIGHT = "38";

    private static void applyPixelGameStyle(Properties properties, String fontSize, String charLength, String charSpace)
    {
        properties.setProperty(KAPTCHA_BORDER, "yes");
        properties.setProperty(KAPTCHA_BORDER_COLOR, PIXEL_BORDER);
        properties.setProperty(KAPTCHA_BORDER_THICKNESS, "1");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, PIXEL_TEXT);
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, CAPTCHA_WIDTH);
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, CAPTCHA_HEIGHT);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, fontSize);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, charLength);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_SPACE, charSpace);
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Monospaced");
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_FROM, PIXEL_BG_FROM);
        properties.setProperty(KAPTCHA_BACKGROUND_CLR_TO, PIXEL_BG_TO);
        properties.setProperty(KAPTCHA_BACKGROUND_IMPL, "com.cat2bug.framework.config.Cat2BugKaptchaBackground");
        properties.setProperty(KAPTCHA_NOISE_COLOR, PIXEL_NOISE);
        properties.setProperty(KAPTCHA_NOISE_IMPL, "com.cat2bug.framework.config.Cat2BugKaptchaNoise");
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.cat2bug.framework.config.Cat2BugKaptchaGimpy");
    }

    @Bean(name = "captchaProducer")
    public DefaultKaptcha getKaptchaBean()
    {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        applyPixelGameStyle(properties, "26", "4", "3");
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
        applyPixelGameStyle(properties, "24", "6", "2");
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "kaptchaCodeMath");
        properties.setProperty(KAPTCHA_TEXTPRODUCER_IMPL, "com.cat2bug.framework.config.KaptchaTextCreator");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
