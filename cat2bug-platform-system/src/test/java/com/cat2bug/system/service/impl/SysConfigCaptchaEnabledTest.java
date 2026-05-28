package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.system.domain.SysConfig;
import com.cat2bug.system.mapper.SysConfigMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 登录验证码开关：空值默认关闭。
 */
@RunWith(MockitoJUnitRunner.class)
public class SysConfigCaptchaEnabledTest
{
    @Mock
    private SysConfigMapper configMapper;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private SysConfigServiceImpl configService;

    @Test
    public void selectCaptchaEnabled_emptyConfig_returnsFalse()
    {
        when(redisCache.getCacheObject(anyString(), anyString())).thenReturn(null);
        when(configMapper.selectConfig(any())).thenReturn(null);
        assertFalse(configService.selectCaptchaEnabled());
    }

    @Test
    public void selectCaptchaEnabled_trueConfig_returnsTrue()
    {
        when(redisCache.getCacheObject(anyString(), anyString())).thenReturn(null);
        SysConfig config = new SysConfig();
        config.setConfigValue("true");
        when(configMapper.selectConfig(any())).thenReturn(config);
        assertTrue(configService.selectCaptchaEnabled());
    }
}
