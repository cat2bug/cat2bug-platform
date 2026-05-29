package com.cat2bug.framework.config;

import java.awt.Color;

/**
 * 与登录页 cat2bug-logo 插画一致的像素验证码色板。
 */
final class Cat2BugKaptchaPalette
{
    /** 太阳光晕亮部 #FFF3D6 */
    static final Color SUN_LIGHT = new Color(255, 243, 214);
    /** 太阳主色 #FFD54F */
    static final Color SUN_MID = new Color(255, 213, 79);
    /** 太阳暗部 #FFC145 */
    static final Color SUN_DEEP = new Color(255, 193, 69);
    /** 笔记本屏幕 #AEE2FF */
    static final Color CYAN = new Color(174, 226, 255);
    /** 花朵 #FF5C8D */
    static final Color PINK = new Color(255, 92, 141);
    /** 绿植/杯子 #4C8259 */
    static final Color GREEN = new Color(76, 130, 89);
    /** 花盆 #8D6E63 */
    static final Color BROWN = new Color(141, 110, 99);
    /** 猫咪灰 #6D6E71 */
    static final Color CAT_GREY = new Color(109, 110, 113);
    /** 插画线稿/表单描边 #5A5A59 */
    static final Color INK = new Color(90, 90, 89);
    /** 笔记本机身 #FCFCFA */
    static final Color WHITE = new Color(252, 252, 250);

    static final String INK_RGB = "90,90,89";
    static final String SUN_LIGHT_RGB = "255,243,214";
    static final String SUN_DEEP_RGB = "255,193,69";

    private Cat2BugKaptchaPalette()
    {
    }
}
