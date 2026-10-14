package com.cat2bug.common.report;

/**
 * 报告编解码器。
 */
public interface IReportCoder<T> {

    String encode(T t);

    T decode(String code);
}
