package com.cat2bug.im.service.impl;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.im.service.IMessageTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 消息模版
 */
@Service
public class DefaultMessageTemplateImpl implements IMessageTemplate<Object> {

    @Override
    public String toText(Object obj, Map<String, Object> params) {
        return String.valueOf(obj);
    }

    @Override
    public String toHtml(Object obj, Map<String, Object> params) {
        return String.valueOf(obj);
    }

    @Override
    public String toMarkdown(Object obj, Map<String, Object> params) {
        return String.valueOf(obj);
    }
}
