package com.cat2bug.im.service;

/**
 * 消息模版
 * @param <T>
 */
public interface IMessageTemplate<T> {
    public String toText(T obj);

    public String toHtml(T obj);

    public String toMarkdown(T obj);
}
