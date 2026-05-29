package com.cat2bug.framework.config;

import java.awt.image.BufferedImage;
import com.google.code.kaptcha.GimpyEngine;
import com.google.code.kaptcha.util.Configurable;

/**
 * 像素风验证码：不做平滑扭曲，保持文字边缘锐利。
 */
public class Cat2BugKaptchaGimpy extends Configurable implements GimpyEngine
{
    @Override
    public BufferedImage getDistortedImage(BufferedImage baseImage)
    {
        return baseImage;
    }
}
