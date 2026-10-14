package com.cat2bug.admin.service;

import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.framework.cache.AppCache;
import com.cat2bug.system.service.SysConfigService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class CaptchaService {

    private static final String CAPTCHA_REGION = "captcha";
    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    @Inject
    SysConfigService sysConfigService;

    @Inject
    AppCache appCache;

    public AjaxResult createCaptchaImage() {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = sysConfigService.selectCaptchaEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        ajax.put("registerEnabled", sysConfigService.selectRegisterEnabled());
        if (!captchaEnabled) {
            return ajax;
        }

        String code = randomCode(4);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        appCache.set(CAPTCHA_REGION, CacheConstants.CAPTCHA_CODE_KEY + uuid, code);

        ajax.put("uuid", uuid);
        ajax.put("captchaExpr", code);
        ajax.put("img", renderBase64Png(code));
        return ajax;
    }

    public void validateCaptcha(String code, String uuid) {
        if (!sysConfigService.selectCaptchaEnabled()) {
            return;
        }
        if (code == null || code.isBlank() || uuid == null || uuid.isBlank()) {
            throw new com.cat2bug.common.exception.ApiException("验证码不能为空");
        }
        String key = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        String cached = appCache.get(CAPTCHA_REGION, key, String.class);
        appCache.remove(CAPTCHA_REGION, key);
        if (cached == null || !cached.equalsIgnoreCase(code.trim())) {
            throw new com.cat2bug.common.exception.ApiException("验证码错误");
        }
    }

    private static String randomCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private static String renderBase64Png(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(64, 158, 255));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            g.drawString(code, 18, 28);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", os);
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (Exception e) {
            throw new com.cat2bug.common.exception.ApiException("验证码生成失败");
        }
    }
}
