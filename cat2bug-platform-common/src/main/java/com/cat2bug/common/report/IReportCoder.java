package com.cat2bug.common.report;

/**
 * 报告编解码器
 * @param <T>
 */
public interface IReportCoder<T> {
    /**
     * 编码
     * @param t
     * @return
     */
    String encode(T t);

    /**
     * 解码
     * @param code
     * @return
     */
    T decode(String code);
}
