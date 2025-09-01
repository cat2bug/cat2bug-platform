package com.cat2bug.im.service;

import java.util.Map;

/**
 * 消息模版
 * @param <T>
 */
public interface IMessageTemplate<T> {
    public String toText(T obj, Map<String, Object> params);

    public String toHtml(T obj, Map<String, Object> params);

    public String toMarkdown(T obj, Map<String, Object> params);
}
