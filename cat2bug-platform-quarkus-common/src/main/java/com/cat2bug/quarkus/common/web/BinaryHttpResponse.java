package com.cat2bug.quarkus.common.web;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 对齐 Servlet HttpServletResponse 的二进制写出子集，供 Git archive/raw/asset 下载使用。
 */
public interface BinaryHttpResponse {

    void setContentType(String contentType);

    void setHeader(String name, String value);

    void setContentLengthLong(long length);

    OutputStream getOutputStream() throws IOException;
}
