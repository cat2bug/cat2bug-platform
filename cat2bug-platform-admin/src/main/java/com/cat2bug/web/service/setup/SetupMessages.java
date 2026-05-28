package com.cat2bug.web.service.setup;

import com.cat2bug.common.utils.MessageUtils;

/**
 * 安装向导 API 文案（随请求头 language 解析为对应 Locale）。
 */
public final class SetupMessages
{
    private SetupMessages()
    {
    }

    public static String msg(String code, Object... args)
    {
        return MessageUtils.message(code, args);
    }
}
