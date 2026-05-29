package com.cat2bug.framework.config;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import com.google.code.kaptcha.NoiseProducer;
import com.google.code.kaptcha.util.Configurable;

/**
 * 2D 像素风干扰：logo 配色碎屑与阶梯折线。
 */
public class Cat2BugKaptchaNoise extends Configurable implements NoiseProducer
{
    private static final int PIXEL = 2;

    private static final Color[] PIXEL_DUST = {
        Cat2BugKaptchaPalette.PINK,
        Cat2BugKaptchaPalette.CYAN,
        Cat2BugKaptchaPalette.GREEN,
        Cat2BugKaptchaPalette.BROWN,
        Cat2BugKaptchaPalette.CAT_GREY
    };

    @Override
    public void makeNoise(BufferedImage image, float x1, float y1, float x2, float y2)
    {
        Graphics2D graph = (Graphics2D) image.getGraphics();
        Cat2BugKaptchaBackground.applyPixelHints(graph);

        int width = image.getWidth();
        int height = image.getHeight();
        Random random = new Random(width * 17L + height * 13L);

        for (int i = 0; i < 8; i++)
        {
            int x = snap(random.nextInt(Math.max(1, width - PIXEL)), PIXEL);
            int y = snap(random.nextInt(Math.max(1, height - PIXEL)), PIXEL);
            graph.setColor(PIXEL_DUST[random.nextInt(PIXEL_DUST.length)]);
            int size = random.nextBoolean() ? PIXEL : PIXEL * 2;
            graph.fillRect(x, y, size, size);
        }

        graph.setColor(Cat2BugKaptchaPalette.INK);
        drawPixelZigZag(graph, 0, snap(random.nextInt(Math.max(1, height - PIXEL * 2)), PIXEL), width, PIXEL);
        drawPixelZigZag(graph, 0, snap(random.nextInt(Math.max(1, height - PIXEL * 2)), PIXEL), width, -PIXEL);

        graph.dispose();
    }

    private static void drawPixelZigZag(Graphics2D graph, int startX, int y, int width, int stepY)
    {
        int x = startX;
        int currentY = y;
        boolean down = stepY > 0;
        while (x < width)
        {
            int segment = Math.min(PIXEL * 3, width - x);
            graph.fillRect(x, currentY, segment, PIXEL);
            x += segment;
            currentY += down ? stepY : 0;
            if (x < width)
            {
                graph.fillRect(x, currentY, PIXEL, PIXEL * 2);
                x += PIXEL;
                currentY -= down ? stepY : 0;
            }
        }
    }

    private static int snap(int value, int grid)
    {
        return Math.max(0, (value / grid) * grid);
    }
}
