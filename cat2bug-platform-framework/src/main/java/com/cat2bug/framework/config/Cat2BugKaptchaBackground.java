package com.cat2bug.framework.config;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import com.google.code.kaptcha.BackgroundProducer;
import com.google.code.kaptcha.util.Configurable;

/**
 * Cat2Bug 品牌风验证码背景：暖黄渐变 + logo 同款「+」与圆点装饰。
 */
public class Cat2BugKaptchaBackground extends Configurable implements BackgroundProducer
{
    private static final Color DECO_GRAY = new Color(90, 90, 89, 48);
    private static final Color DECO_BLUE = new Color(174, 226, 255, 140);
    private static final Color SUN_GLOW = new Color(255, 213, 79, 72);

    @Override
    public BufferedImage addBackground(BufferedImage baseImage)
    {
        Color from = getConfig().getBackgroundColorFrom();
        Color to = getConfig().getBackgroundColorTo();
        int width = baseImage.getWidth();
        int height = baseImage.getHeight();

        BufferedImage imageWithBackground = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graph = (Graphics2D) imageWithBackground.getGraphics();
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graph.setPaint(new GradientPaint(0, 0, from, width, height, to));
        graph.fillRect(0, 0, width, height);

        Random random = new Random();
        int glowX = width - height / 2;
        int glowY = height / 2;
        int glowSize = Math.max(height, width / 3);
        graph.setColor(SUN_GLOW);
        graph.fill(new Ellipse2D.Float(glowX - glowSize / 2f, glowY - glowSize / 2f, glowSize, glowSize));

        graph.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graph.setFont(new Font("Arial", Font.BOLD, Math.max(8, height / 4)));
        for (int i = 0; i < 7; i++)
        {
            int x = random.nextInt(Math.max(1, width - 8)) + 4;
            int y = random.nextInt(Math.max(1, height - 4)) + 4;
            graph.setColor(i % 2 == 0 ? DECO_BLUE : DECO_GRAY);
            if (i % 3 == 0)
            {
                drawPlus(graph, x, y, Math.max(3, height / 8));
            }
            else
            {
                graph.fillOval(x, y, 3, 3);
            }
        }

        graph.drawImage(baseImage, 0, 0, null);
        graph.dispose();
        return imageWithBackground;
    }

    private static void drawPlus(Graphics2D graph, int x, int y, int size)
    {
        graph.drawLine(x - size, y, x + size, y);
        graph.drawLine(x, y - size, x, y + size);
    }
}
