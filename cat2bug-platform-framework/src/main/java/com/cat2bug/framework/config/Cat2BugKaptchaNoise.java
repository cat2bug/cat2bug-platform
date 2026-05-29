package com.cat2bug.framework.config;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import com.google.code.kaptcha.NoiseProducer;
import com.google.code.kaptcha.util.Configurable;

/**
 * Cat2Bug 品牌风验证码干扰线：轻量曲线与「+」点缀，保持可读性。
 */
public class Cat2BugKaptchaNoise extends Configurable implements NoiseProducer
{
    @Override
    public void makeNoise(BufferedImage image, float x1, float y1, float x2, float y2)
    {
        Graphics2D graph = (Graphics2D) image.getGraphics();
        graph.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graph.setColor(getConfig().getNoiseColor());
        graph.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int width = image.getWidth();
        int height = image.getHeight();
        Random random = new Random();

        for (int i = 0; i < 2; i++)
        {
            int y = random.nextInt(Math.max(1, height));
            int wave = random.nextInt(5) - 2;
            graph.drawLine(0, y, width, y + wave);
        }

        graph.setFont(new Font("Arial", Font.BOLD, Math.max(7, height / 5)));
        for (int i = 0; i < 3; i++)
        {
            int x = random.nextInt(Math.max(1, width - 6));
            int y = random.nextInt(Math.max(1, height - 2)) + 2;
            graph.drawString("+", x, y);
        }

        graph.dispose();
    }
}
