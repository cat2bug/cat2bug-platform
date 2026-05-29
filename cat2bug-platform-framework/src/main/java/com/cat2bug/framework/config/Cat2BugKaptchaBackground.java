package com.cat2bug.framework.config;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import com.google.code.kaptcha.BackgroundProducer;
import com.google.code.kaptcha.util.Configurable;

/**
 * 2D 像素风验证码背景：logo 同款暖黄太阳光晕与场景元素。
 */
public class Cat2BugKaptchaBackground extends Configurable implements BackgroundProducer
{
    private static final int PIXEL = 4;

    @Override
    public BufferedImage addBackground(BufferedImage baseImage)
    {
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();

        BufferedImage imageWithBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graph = (Graphics2D) imageWithBackground.getGraphics();
        applyPixelHints(graph);

        int shelfTop = height - PIXEL * 2;
        Color[] sunBands = { Cat2BugKaptchaPalette.SUN_LIGHT, Cat2BugKaptchaPalette.SUN_MID, Cat2BugKaptchaPalette.SUN_DEEP };
        for (int y = 0; y < shelfTop; y += PIXEL)
        {
            graph.setColor(sunBands[(y / PIXEL) % sunBands.length]);
            graph.fillRect(0, y, width, PIXEL);
        }

        Random random = new Random(width * 31L + height);
        drawPixelFlower(graph, PIXEL * 2, shelfTop - PIXEL * 3);
        drawPixelLaptop(graph, width / 2 - PIXEL * 3, shelfTop - PIXEL * 4);
        drawPixelCup(graph, width - PIXEL * 8, shelfTop - PIXEL * 5);
        drawPixelCat(graph, width / 2 - PIXEL, shelfTop - PIXEL * 2);

        graph.setColor(Cat2BugKaptchaPalette.BROWN);
        graph.fillRect(0, shelfTop, width, PIXEL);
        graph.setColor(Cat2BugKaptchaPalette.GREEN);
        graph.fillRect(0, shelfTop + PIXEL, width, height - shelfTop - PIXEL);

        graph.drawImage(baseImage, 0, 0, null);
        graph.dispose();
        return imageWithBackground;
    }

    private static void drawPixelFlower(Graphics2D graph, int x, int y)
    {
        graph.setColor(Cat2BugKaptchaPalette.PINK);
        graph.fillRect(x, y, PIXEL, PIXEL);
        graph.fillRect(x - PIXEL, y + PIXEL / 2, PIXEL, PIXEL);
        graph.fillRect(x + PIXEL, y + PIXEL / 2, PIXEL, PIXEL);
        graph.setColor(Cat2BugKaptchaPalette.SUN_MID);
        graph.fillRect(x, y + PIXEL, 2, 2);
        graph.setColor(Cat2BugKaptchaPalette.GREEN);
        graph.fillRect(x, y + PIXEL + 2, 2, PIXEL);
        graph.setColor(Cat2BugKaptchaPalette.BROWN);
        graph.fillRect(x - 2, y + PIXEL * 2, PIXEL + 4, PIXEL);
    }

    private static void drawPixelLaptop(Graphics2D graph, int x, int y)
    {
        graph.setColor(Cat2BugKaptchaPalette.WHITE);
        graph.fillRect(x, y + PIXEL, PIXEL * 3, PIXEL);
        graph.setColor(Cat2BugKaptchaPalette.CYAN);
        graph.fillRect(x + PIXEL / 2, y, PIXEL * 2, PIXEL);
        graph.setColor(Cat2BugKaptchaPalette.INK);
        graph.drawRect(x, y, PIXEL * 3, PIXEL * 2);
    }

    private static void drawPixelCup(Graphics2D graph, int x, int y)
    {
        graph.setColor(Cat2BugKaptchaPalette.GREEN);
        graph.fillRect(x, y, PIXEL, PIXEL * 2);
        graph.fillRect(x + PIXEL, y + PIXEL / 2, PIXEL, PIXEL);
        graph.setColor(Cat2BugKaptchaPalette.INK);
        graph.drawRect(x, y, PIXEL * 2, PIXEL * 2);
    }

    private static void drawPixelCat(Graphics2D graph, int x, int y)
    {
        graph.setColor(Cat2BugKaptchaPalette.CAT_GREY);
        graph.fillRect(x, y, PIXEL * 2, PIXEL);
        graph.fillRect(x - PIXEL / 2, y - PIXEL / 2, PIXEL / 2, PIXEL / 2);
        graph.fillRect(x + PIXEL * 2, y - PIXEL / 2, PIXEL / 2, PIXEL / 2);
        graph.setColor(Cat2BugKaptchaPalette.INK);
        graph.fillRect(x + PIXEL / 2, y + PIXEL / 4, 2, 2);
        graph.fillRect(x + PIXEL + 2, y + PIXEL / 4, 2, 2);
    }

    static void applyPixelHints(Graphics2D graph)
    {
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graph.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graph.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graph.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }
}
