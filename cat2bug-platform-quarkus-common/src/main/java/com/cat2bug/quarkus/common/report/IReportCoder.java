package com.cat2bug.quarkus.common.report;

/**
 * 报告编解码器。
 */
public interface IReportCoder<T> {

    String encode(T t);

    T decode(String code);
}
